# 운영 영향도 분석 (2026-08-25)

`audit-findings-2026-08.md`의 발견 항목을 **"실제 운영에서 터졌을 확률"** 기준으로 재평가한 것.
심각도 라벨과 실제 발생 가능성은 다르므로, 수정 우선순위는 이 문서를 기준으로 정한다.

---

## 0. 이미 발생한 사건 — 잘못된 테스트가 원인 (검증됨)

### SSE 실시간 위치 전송이 3일 이상 죽어 있었다

git 히스토리에 남아 있는 **실제 장애**다. 이 프로젝트에서 "테스트를 잘못 써서 프로덕션 버그가
통과한" 가장 명확한 사례이므로 반드시 참고할 것.

타임라인:
- `b5e62dc` (2026-08-06) — `SseEmitterManagerTest` 생성. 통과.
- `6ee7b30` (2026-08-09) — "SSE 실시간 위치 전송 불가 버그 수정". **3일 후**

그 3일 동안 테스트는 계속 초록불이었다. 커밋 메시지에 적힌 근본 원인:

> `Gps` record의 파생 getter `isAccurate()`가 직렬화 시 `accurate` 필드를 생성하는데
> record 컴포넌트가 아니어서, Redis 캐시 역직렬화 시 `UnrecognizedPropertyException`이 발생해
> `GpsLatestCacheManager.find()`가 항상 `Optional.empty()`를 반환했다.
> 그 결과 SSE 폴링 루프가 heartbeat만 보내고 location 이벤트를 한 번도 전송하지 못했다.

**왜 테스트가 못 잡았는가 — 인과관계가 명확하다:**

1. `GpsLatestCacheManagerTest`가 `ObjectMapper`를 `@Mock`으로 대체한다.
   → 실제 Jackson 직렬화/역직렬화 왕복이 **한 번도 실행되지 않는다.**
   깨진 지점이 정확히 그 왕복이었다.
2. `find_returns_gps_when_cache_exists`는 스텁이 준 `expected`를 그대로 비교하는
   동어반복이라, 역직렬화가 100% 실패해도 통과한다.
3. `GpsLatestCacheManager.find()`의 catch가 **무음**이었다(로그 없음).
   → 운영 로그에도 흔적이 남지 않았다.
4. `SseEmitterManagerTest`는 `connect()`가 emitter를 반환하는지(`isNotNull()`)만 확인하고
   **실제로 location 이벤트가 전송되는지는 검증하지 않는다.**
   heartbeat만 나가는 상태와 정상 상태를 구분하지 못한다.

**교훈: 직렬화 왕복을 검증해야 하는 자리에서 `ObjectMapper`를 mock하면 안 된다.**
실제 `ObjectMapper` 인스턴스로 `writeValueAsString` → `readValue` 왕복을 단언해야 한다.

**현재도 미해결:** `@JsonIgnoreProperties(ignoreUnknown = true)`로 증상은 막았지만,
`GpsLatestCacheManagerTest`는 **여전히 ObjectMapper를 mock**하고 있고 `Gps`의 실제
Jackson 왕복을 검증하는 테스트는 아직 없다. `Gps`에 파생 getter를 하나 더 추가하면
같은 계열의 버그가 재발할 수 있다.

---


### ② 탈퇴한 회원의 GPS가 30일간 계속 수집됨 (A-2)

공격 불필요. **탈퇴한 모든 WARD에게 자동으로 발생**했다.
탈퇴 시 `gpsHistoryManager.deleteByWardMemberKey`로 과거 이력은 지우지만,
device token 캐시가 살아 있어 **지운 다음부터 새 위치가 다시 쌓인다.**

개인정보보호법상 파기 의무 위반 소지가 있는 구간이다.
→ **확인 방법**: `member_withdrawals`의 탈퇴자 memberKey로 `gps_histories`에 잔존 행이 있는지 조회.

### ③ 공동 보호자 추가 API가 한 번도 성공한 적 없음 (A-8)

간헐적 오류가 아니라 **결정론적 100% 실패**. 프론트에 버튼이 연결돼 있었다면
전부 실패했다. QA에서 안 걸렸다면 해당 화면이 미연결이거나 사용자가 문의하지 않은 것.

---

## 2순위 — 실재하지만 트리거가 필요함

### ④ 로그아웃이 실제로는 아무것도 무효화하지 않음 (A-1 재해석)

> ⚠️ A-1을 "탈취 시 14일 접근"으로 설명하면 **과장**이다.
> refresh token을 이미 훔쳤다면 `/auth/refresh`로 정상 access token을 받으면 그만이라
> 추가 피해가 크지 않다.

진짜 문제는 폐기 수단이 없다는 것이다:

```java
// LocalAuthService.java:60
public void signOut(String refreshToken, String fcmToken) {
    refreshTokenWriter.delete(refreshToken);   // DB 행만 삭제
```

`JwtValidator`는 **DB를 조회하지 않고** 서명·만료만 본다.
따라서 로그아웃으로 DB 행을 지워도 그 refresh token 문자열을 `Authorization: Bearer`로
보내면 여전히 통과한다. → 로그아웃 후에도 최대 14일간 `.authenticated()` 엔드포인트
(`GET/PATCH /api/v1/members/me` 등) 접근이 살아 있다.

실제 피해 시나리오: 공용 기기·분실 단말에서 로그아웃한 경우.
**이 버그의 본질은 "토큰 혼용"이 아니라 "토큰 폐기 수단 부재"다.**

### ⑤ SMS 인증 무차별 대입 + 발송 과금

6자리(10⁶), TTL 5분, 시도 횟수 제한 없음, 실패해도 코드 미삭제, 엔드포인트는 `permitAll()`.
`sendCode`에도 쿨다운이 없어 스크립트 한 번에 **실제 CoolSMS 요금이 발생**한다.

→ **확인 방법**: CoolSMS 콘솔의 최근 발송량. 비정상 급증이 있으면 이미 악용된 것.

### ⑥ 안심존 IDOR (A-3)

`safeZoneKey`가 UUID라 외부 공격자의 추측은 어렵다.
다만 보호자가 최대 5명의 피보호자를 관리하므로, 프론트에서 ward 전환 시 이전 ward의
`safeZoneKey`가 남아 있으면 **사용자가 의도치 않게 다른 피보호자의 안심존을 수정·삭제**할 수 있다.
공격보다 이쪽이 현실적이다.

---

## 실질 영향이 낮은 것

심각도 라벨에 비해 실제 발생 가능성이 낮다. 수정은 하되 우선순위를 낮춘다.

| 항목 | 이유 |
|---|---|
| ward 5명 한도 우회 (A-9) | 초대 6건을 미리 뿌려두고 동시 수락시켜야 함. 우연히 발생하지 않음 |
| 약관 미동의 가입 (A-5) | 정상 앱은 항상 세 필드를 보냄. API 직접 호출이 필요 |
| 구독 게이트 무력화 (A-10) | `validatePremium` 호출부가 전부 주석 처리 → 수익화 미가동. **유료화를 켜는 시점에** 문제가 됨 |
| 만료 초대 미구현 (A-11) | 오래된 초대장이 남는 정도 |
| `@Valid` 누락 (A-7) | 정상 앱은 빈 토큰을 보내지 않음 |

## 헛다리 (조사 완료, 문제 아님)

- **`GpsHistory.recordedAt`** — `GpsHistoryRepositoryTest`의 `buildHistory()`가 이 필드를
  세팅하지 않아 테스트가 무의미하지만, 프로덕션 `GpsHistoryManager.java:36`은
  `.recordedAt(gps.recordedAt())`을 제대로 넣는다. **저장 경로는 정상.** 테스트만 고치면 된다.

---

## 잘못된 테스트가 "허위 보증"을 준 사례들

위 SSE 건 외에도, 테스트가 존재하고 통과했기 때문에 **문제없다고 믿게 만든** 자리들이다.
단순 커버리지 공백(테스트가 아예 없음)과 구분해서 봐야 한다.

| 테스트 | 무엇을 보증한다고 주장했나 | 실제 |
|---|---|---|
| `CareRelationshipValidatorTest:163` `validateCanAddGuardian_success` | "공동 보호자가 없으면 정상 통과한다" | 실 DB에서 불가능한 mock 조합. **항상 실패하는 API를 정상으로 인증** |
| `MemberWithdrawalManagerTest` `withdraw_success` | "연관 데이터를 **모두** 삭제한다" | verify 목록이 구현을 한 줄씩 베낀 미러. 누락을 **구조적으로 발견할 수 없음** |
| `GpsHistoryRepositoryTest` `..._excludes_other_wards` | "다른 wardKey는 조회 결과에 포함되지 않는다" | 결과가 항상 빈 리스트 → `noneMatch` 무조건 통과. **쿼리에서 wardKey 필터를 지워 전 피보호자 위치가 반환돼도 통과** |
| `LocationSettingControllerTest:95` `updateCollectionInterval_...` | "수집 주기 수정이 반영된다" | PATCH 값이 기본값(30)과 동일 → **핸들러 본문을 지워도 통과.** 주기 변경이 실제로 저장 안 되면 WARD 단말 배터리가 빨리 닳아 위치 추적 자체가 멈춤 |
| `DeviceTokenControllerTest:86` `issueToken_returns_200_on_second_request` | "재발급이 동작한다" | 첫 토큰과 다른지 비교하지 않음. **탈취된 토큰을 회전으로 무효화하지 못해도 통과** |
| `FcmDeviceTokenControllerTest:147` `upsert_returns_400_for_blank_token` | "빈 토큰은 400" | 단언은 정확하나 **CI가 통합 테스트를 안 돌려 한 번도 실행되지 않음.** 저장소에는 커버리지가 있는 것처럼 보임 |
| `MemberServiceTest:146` `updateMyInfo_success_with_password` | "비밀번호 변경이 동작한다" | current/new에 같은 값 + `any()` 검증 → **두 인자가 뒤바뀌어도 통과** |

---

## 조치 전 확인할 것 (코드 수정 없이 판별 가능)

1. **ECS 태스크 정의의 `FIREBASE_ENABLED`** — 없으면 그동안 푸시가 전부 안 나갔다
2. **CoolSMS 최근 발송량** — 비정상 급증 시 ⑤가 이미 악용된 것
3. **탈퇴자 memberKey의 `gps_histories` 잔존 행** — ②의 실제 피해 규모

세 가지 결과에 따라 수정 우선순위가 크게 달라진다.
