# 테스트 감사 발견 백로그 (2026-08-25)

전 도메인(auth, care, member, device, location, safezone, notification, sms, security, common)
테스트 코드 감사 결과. 감사 시점에 `./gradlew test`는 100% 통과 중이었다.

**상태: 미조치.** 아래 항목은 발견만 된 상태이며 수정되지 않았다.
착수 전 각 항목이 아직 유효한지 해당 파일을 다시 확인할 것.

패턴별 일반화는 `../test-antipatterns.md` 참고.

---

## A. 검증된 프로덕션 버그

프로덕션 코드를 직접 읽어 확인한 것만 기재한다.

### A-1. Refresh token이 access token으로 통용된다
- `security/jwt/JwtGenerator.java:39-58`
- access/refresh가 **같은 `secretKey`로 서명**되고 종류를 구분하는 클레임(`typ`/`aud`)이 없다.
  차이는 access에만 `role` 클레임이 있다는 것뿐.
- `JwtValidator.validate()`는 서명·만료만 검사하고, `JwtAuthenticationFilter:54-64`는
  통과한 토큰의 subject를 그대로 인증 주체로 세운다.
- 결과: 탈취한 refresh token(유효기간 14일)을 `Authorization: Bearer`로 보내면 인증 성립.
  `role`이 null이라 `ROLE_null`이 되어 `hasRole(...)` 경로는 막히지만,
  `SecurityConfig.java:117`의 `.anyRequest().authenticated()` 버킷은 전부 통과한다
  (`GET/PATCH /api/v1/members/me`, `POST /api/v1/auth/oauth/link/*` 등).
- 수정 방향: `typ` 클레임 추가 + `JwtValidator`에서 용도 검증.
- 테스트 갭: access/refresh 구분을 단언하는 테스트가 없다. 더 근본적으로
  `JwtAuthenticationFilter` / `DeviceTokenAuthFilter` 테스트가 **0건**이다.

### A-2. 탈퇴한 회원의 device token이 최대 30일간 유효하다
- `member/implement/MemberWithdrawalManager.java` — `wardDeviceTokenRepository.deleteByWardKey()`로
  **DB 행만** 지운다.
- `device/implement/WardDeviceTokenReader.java:22-26` — 캐시 히트 시 DB를 조회하지 않는다. TTL 30일(`:17`).
- `evict()`의 호출처는 프로덕션 전체에서 `WardDeviceTokenManager:22`(재발급 경로) 한 곳뿐.
- 결과: 탈퇴 직후에도 해당 device token으로 `POST /api/v1/location/gps`가 200을 반환한다.
- 수정 방향: `withdraw()`에서 `wardDeviceTokenReader.evict(token)` 호출.
  단, 토큰 값을 먼저 조회해야 하므로 삭제 순서 주의.
- 테스트 갭: `MemberWithdrawalManagerTest`에 `WardDeviceTokenReader` mock 필드 자체가 없다.
  검증 목록이 구현을 한 줄씩 베낀 미러라, 구현에 없는 호출은 구조적으로 요구할 수 없다.

### A-3. 안심존 IDOR — 남의 피보호자 안심존 조회·수정·삭제 가능
- `safezone/business/SafeZoneService.java:39,45,51`
- `getSafeZone`/`updateSafeZone`/`deleteSafeZone`이 `wardKey`에 대한 권한만 검증하고,
  이어서 `findBySafeZoneKey(safeZoneKey)` / `update(safeZoneKey, ...)` / `delete(safeZoneKey)`를
  **키 단독으로** 호출한다. `safeZoneKey`가 그 `wardKey` 소유인지 확인하지 않는다.
- `SafeZoneWriter.java:38`, `SafeZoneReader.java:30`의 `getEntity`도 키로만 조회.
- 결과: A의 보호자가 `/api/v1/care/wards/{A의 wardKey}/safe-zones/{B의 safeZoneKey}`로
  요청하면 무관한 피보호자 B의 안심존에 접근된다.
- 수정 방향: 조회한 엔티티의 `wardMemberKey == wardKey` 검증 추가.
- 테스트 갭: `SafeZoneServiceTest`의 수정/삭제 테스트가 전부 `"any-key"` 또는 같은 ward의
  fixture 키만 넘긴다. `SafeZoneFixture.createSafeZone(String wardMemberKey)`라는
  다른 ward용 팩토리가 이미 있는데 이 시나리오에 쓰이지 않았다.

### A-4. Device token에 만료가 없다
- `device/dataaccess/entity/WardDeviceToken.java:39` — `expiresAt`이 프로덕션 전체에서
  **선언만 있고 읽기도 쓰기도 되지 않는다.**
- `WardDeviceTokenReader.getByToken()`에도 만료 검사가 없다.
- 결과: WARD 백그라운드 앱의 장기 인증 토큰이 무기한 유효.
- 정책 결정 필요: 만료 기간을 정할지, 필드를 제거할지.

### A-5. 약관 동의 없이 가입할 수 있다
- `auth/controller/request/SignUpRequest.java:23-25`
- `@AssertTrue Boolean isTermsOfServiceAgreed` — Bean Validation 스펙상 `@AssertTrue`는
  **값이 null이면 통과**한다. 박싱 타입인데 `@NotNull`이 없다.
- 결과: 요청 바디에서 세 필드를 빼면 약관 미동의 상태로 가입 성공.
- 수정 방향: `@NotNull` 병기 또는 primitive `boolean`으로 변경.
- 테스트 갭: `AuthControllerTest.signUp_success`가 세 필드를 모두 `true`로 보내는 경우만 있다.

### A-6. CI가 통합 테스트를 한 번도 실행하지 않는다
- `.github/workflows/ci-check.yml:32`, `deploy.yml:46` — 둘 다 `./gradlew clean test`만 실행.
- `build.gradle:98`이 `excludeTags 'integration'`, `integrationTest` 태스크(`:104`)는 미호출.
- 그 결과 아래 두 건이 **깨진 채 방치**되어 있다:
  - `NotificationSettingControllerTest.java:262` — Swagger summary를 `"Get notification settings"`(영문)로
    기대하지만 `NotificationSettingController`의 실제 값은 `"알림 설정 조회"`(한국어).
    실행하면 즉시 실패한다.
  - `location/dataaccess/GpsHistoryRepositoryTest.java` — `buildHistory()`가 `recordedAt`을
    세팅하지 않는데 `GpsHistory.recordedAt`은 `@Column(nullable=false)`이고 `@CreatedDate`가 **없다**
    (테스트 주석은 자동 설정된다고 잘못 적혀 있다). NOT NULL 위반으로 실패할 가능성이 높다.
    ※ Docker 미기동으로 실증하지 못함 — 착수 시 먼저 실행해 확인할 것.
- 역설: `NotificationSettingSwaggerHttpTest`는 `@Tag("integration")`이 **없어서**
  매 CI마다 Tomcat을 띄우며 실행된다. 실행되는 유일한 무거운 테스트가 가치는 가장 낮다.

### A-7. `@Valid` 누락 — 공백 FCM 토큰이 저장된다
- `notification/controller/FcmDeviceTokenController.java:34` — `@RequestBody`에 `@Valid`가 없다.
  프로젝트의 다른 컨트롤러 20곳은 전부 붙어 있다.
- `UpsertFcmDeviceTokenRequest`의 `@NotBlank @Size(max=512)`가 전혀 동작하지 않는다.
- 결과: `token: " "`가 200으로 저장되어 `unique` 컬럼 한 자리를 차지한다.
- 이를 잡을 유일한 테스트(`FcmDeviceTokenControllerTest.java:147`)가 A-6 때문에 실행되지 않는다.

### A-8. 공동 보호자 추가 API가 항상 실패한다
- `care/implement/CareRelationshipValidator.java:51-58`
- `validateGuardianRole()`을 통과했다는 것은 `(ward, requester, GUARDIAN)` 행이 존재한다는 뜻인데,
  바로 다음 줄의 `findAllByWardMemberKey(wardMemberKey)`가 그 행을 반드시 포함한다.
  `checkRoleLimit(..., GUARDIAN, MAX_GUARDIAN_COUNT=1, ...)`에서 `count >= 1`이 항상 성립.
- 결과: `POST /api/v1/care/requests/guardian`는 어떤 입력으로도 성공할 수 없다.
- **정책 결정 필요**: 요청자 본인을 한도 계산에서 제외할지, `MAX_GUARDIAN_COUNT`를 2로 올릴지.
- 테스트 갭: `CareRelationshipValidatorTest.java:163`이 `existsCareRelationship → true`와
  `findAllByWardMemberKey → List.of()`를 **동시에** stub한다. 실 DB에서 성립 불가능한 조합이
  죽은 코드를 초록불로 덮었다. `CareControllerTest`에도 이 엔드포인트 테스트가 없다.

### A-9. ward 5명 한도 우회 (TOCTOU)
- `care/implement/CareInvitationManager.java:38` — 한도 검증이 **초대 발송 시점**에만 수행된다.
- `care/implement/CareRelationshipWriter.java:28` — 수락 시 재검증은
  `member.getRole() == MemberRole.GUARDIAN`일 때만 수행된다.
- ward 초대의 수신자는 **WARD**이므로 `accept()`의 `memberKey`는 ward의 키이고,
  역할이 `WARD`라 재검증 분기를 타지 않는다.
- 결과: 관계 0개 상태에서 초대 6건을 연속 발송하면 각 시점마다 `count=0 < 5`로 전부 통과하고,
  6명이 모두 수락하면 관계가 6개 생성된다.
- 테스트 갭: `CareRelationshipWriter.register()`는 테스트가 0건이다.

### A-10. 구독 게이트가 죽은 코드
- `member/implement/MemberValidator.java:17` — `if (member.getSubscriptionType() == null) throw`
- `member/dataaccess/entity/Member.java:63` — 생성자가 무조건 `SubscriptionType.BASIC`을 대입하고
  컬럼도 `nullable = false`.
- 결과: 어떤 회원에 대해서도 예외를 던지지 않는다. `validatePremium`은 호출부가 전부
  주석 처리되어 있다(`CareRelationshipValidator.java:45,54`).
- **정책 결정 필요**: 살릴지(BASIC 차단 기준 필요) vs 제거할지.
- 테스트 갭: `MemberValidator`는 테스트 파일이 없다.

### A-11. 만료된 케어 초대가 미구현
- `support/exception/ErrorType.java:64`에 `EXPIRED_CARE_REQUEST(E5005, "만료된 케어 요청입니다.")`가
  정의되어 있으나 **프로덕션 전체에서 한 번도 throw되지 않는다**(grep 확인).
- `CareInvitation` 엔티티에 `expiresAt` 필드도, 만료 판정 로직도 없다.
- 결과: 1년 전 초대장도 영구히 수락 가능하다.

---

## B. 미검증 — 에이전트 보고만 있는 항목

착수 전 직접 확인할 것.

- **`PhoneVerificationService`** — 인증코드 브루트포스·재발송 제한이 구현에 없음.
  코드 불일치 시 저장된 코드를 지우지 않고 시도 횟수 카운터도 없다(6자리, TTL 5분).
  `sendCode`에도 쿨다운이 없고 해당 엔드포인트는 `permitAll()` → SMS 폭탄·과금 공격 가능.
- **`FcmClient` 빈 선택이 두 개의 다른 조건 축을 씀** — `NoOpFcmClient`는
  `@ConditionalOnProperty(firebase.enabled=false, matchIfMissing=true)`,
  `FirebaseFcmClient`는 `@ConditionalOnBean(FirebaseMessaging.class)`.
  운영에서 `FIREBASE_ENABLED` 주입이 누락되면 NoOp이 조용히 선택되어 **푸시가 전멸**하고
  에러 로그도 남지 않는다(상위 계층은 성공으로 인식).
- **`FcmDeviceTokenRepositoryCustomImpl.findTokensByCareRoles`** — 두 컬렉션이 모두 비면
  `BooleanBuilder`가 비어 `WHERE` 절 없는 전체 SELECT가 된다.
  현재는 `CareRole`이 2개뿐이고 리스너가 조기 반환해 도달 불가하나, `CareRole` 추가 시 재난.
- **`MemberService.updateMyInfo` 순서** — `updateProfile()`을 먼저 실행하고 그 다음
  `verifyPassword()`를 호출한다(`MemberService.java:58-63`). 현재는 `@Transactional` 롤백에만 의존.
- **`NotificationSettingValidator`** — WARD가 다른 WARD의 설정에 접근하는 경로가
  막히는지 미검증(`validateSelfAccess`의 `if (!requesterKey.equals(wardKey))`).

---

## C. 테스트가 0건인 클래스 (우선순위 순)

| 클래스 | 위험도 | 이유 |
|---|---|---|
| `JwtAuthenticationFilter` | 최상 | 실제 인증 강제 지점. `shouldNotFilter` 경로 오타 시 JWT 검증이 통째로 사라짐 |
| `DeviceTokenAuthFilter` | 최상 | WARD GPS의 유일한 인증 관문. 경로 목록이 위 필터와 수동 동기화됨 |
| `GpsHistoryManager` | 최상 | GPS 전 데이터의 쓰기 경로. `Gps` VO 7필드 → 엔티티 매핑이 무방비 (`accuracy`↔`speed`, `recordedAt`↔`measuredAt`) |
| `LocationValidator.validateHistoryViewAccess` | 상 | `GET /location/history/{wardKey}`의 유일한 인가 게이트. 메서드 자체가 테스트에서 한 번도 호출되지 않음 |
| `MemberValidator` | 상 | 비즈니스 규칙 담당인데 실제로는 A-10처럼 죽은 코드였음 |
| `FirebaseFcmClient` | 상 | 부분 실패 루프·무효 토큰 판정·3회 재시도가 전부 여기. 판정이 잘못 넓어지면 멀쩡한 토큰이 **DB에서 삭제**됨 |
| `CareRelationshipCacheReader` | 상 | 모든 위치 인가의 실제 판정부. `@Cacheable` 키 접미사(`:CAREGIVER`/`:GUARDIAN`)를 지우면 MANAGER가 GUARDIAN 권한을 캐시 히트로 획득 |
| `SafeZoneNotificationListener` | 중상 | 안심존 알림 발송. 형제 리스너 2개는 테스트가 있음 |
| `CareInvitationReader` / `CareInvitationWriter` | 중상 | accept/reject 상태 전이가 단위·통합 어디서도 미검증 |
| `WardDeviceTokenManager` | 중상 | 토큰 회전 + 캐시 evict 전체 |
| `NaverAuthenticator` / `KakaoAuthenticator` | 중 | `NaverUser.toOAuthUser()`가 `response.id()`를 null 체크 없이 호출 → 네이버 에러 응답 시 NPE 500 |
| `CookieService.create()` | 중 | `httpOnly`/`secure` 제거, `maxAge` 단위 오류(14일→38년)를 잡을 테스트 없음 |
| `SmsCodeGenerator` | 중 | `%06d` 포맷·범위가 `SmsCode` 정규식(`^\d{6}$`)과 어긋나면 런타임 예외 |
| `MaskingUtils` | 하 | PII 마스킹. `localPart.length() <= 3` 경계, `@` 복수 이메일 미검증 |

---

## D. 구조적 문제

- **`AbstractRepositoryTest`에 `@Tag("integration")`이 없다.** `AbstractIntegrationTest`는
  `@IntegrationTest` 메타 애노테이션으로 상속시키는데 비대칭이다. 태그를 잊은 신규 repository
  테스트는 에러 없이 조용히 `./gradlew test` 안에서 Postgres 컨테이너를 띄운다.
  → `AbstractRepositoryTest`에도 태그를 올리는 것이 맞다.
- **통합 테스트 간 상태 격리가 없다.** `AbstractIntegrationTest`에 `@Transactional`도
  정리 로직도 없어 Postgres 데이터가 누적된다. Redis는 sms 테스트 2곳만 `@AfterEach`에서
  `flushAll()`을 부르는데, 같은 컨테이너를 쓰는 다른 도메인 데이터까지 전부 날린다.
- **`MemberWithdrawalManager`가 타 도메인 Repository 4개를 직접 주입받는다**
  (auth/notification/location/device). 아키텍처 규칙(도메인 경계) 위반이라 별건 검토 필요.
- **`NotificationSettingInfo`가 `business/` 패키지에 있다.** "business 아래 데이터 전달 DTO 금지"
  규칙 위반이며, 내부에 중첩 record 4개가 있어 "중첩 타입 금지"도 함께 위반한다.
- **`SafeZoneServiceTest`가 `com.recaring.safezone` 패키지에 있다.** 대상 클래스는
  `com.recaring.safezone.business`. 같은 도메인의 다른 테스트는 전부 프로덕션 패키지를 미러링한다.
- **`WardDeviceTokenTest`가 `com.recaring.device`에 있다.** 대상은
  `com.recaring.device.dataaccess.entity`.
- **`auth/business/command/SendCodeCommand.java`, `VerifyCodeCommand.java`** — 파일 내용이
  `// deleted` 한 줄뿐인 껍데기다. 삭제 대상.
- **`support/JpaAuditingTestConfig.java`** — 이 클래스를 `@Import`하는 테스트가 0건이다.
  `@EnableJpaAuditing`은 이미 `RecaringApplication`에 있다. 삭제 대상.
- **`NotificationSetting` 엔티티의 변경 메서드 4개**(`updateSafeZone`/`updateAnomaly`/
  `updateEmergencyCall`/`updateBattery`)가 커밋 b589413 이후 `src/main` 어디에서도 호출되지 않는다.
  QueryDSL 경로와 공존하면 실수로 엔티티 쪽을 다시 써서 lost update가 부활할 수 있다.
