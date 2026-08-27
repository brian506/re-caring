# 감사 기록 2026-08-27 — auth/implement, member 신규 테스트

탐색만 하고 수정은 보류. 나중에 도메인 단위로 다시 불러 고칠 때 이 파일을 시작점으로 쓴다.
(이 파일 자체가 `test-antipatterns.md`가 가리키는 `audit/` 위치의 신규 스냅샷이다.)

> **정책 (최종, 같은 날 확정):**
> - VO / Implement / Business → 단위 테스트
> - **Controller → Testcontainers 통합 테스트** (`AbstractIntegrationTest` 상속). Mock 기반(`@WebMvcTest`) 금지
> - Repository / Entity → 별도 테스트 파일 만들지 않음
> - 외부 HTTP 연동은 가짜 서버(`MockRestServiceServer`/`MockWebServer`) 대신 **목이 결과 객체를 그대로 반환**
>
> `test-design.md`·`forbidden.md`·`SKILL.md` 갱신 완료.
>
> ⚠️ 중간에 "Controller도 제외"로 잘못 적용해 `MemberControllerTest`(422줄)를 삭제했다가 되살렸다.
> git 미추적 상태였어서 복구 불가 → 스펙을 재구성해 새로 작성함. `MyInfoResponseTest`(Response record 단위 테스트),
> `MemberTest`(Entity), `MemberRepositoryTest`(Repository)는 정책상 삭제가 맞아 그대로 둔다.

---

## 1. `auth/implement` — 기존 구현된 테스트 9개 파일 (수정 전, 감사만)

기준: `test-design.md` §1 "이 코드가 결정을 내리는가?" + `test-antipatterns.md` 체크리스트.
전 파일 공통: `// SPEC`/`// IMPL` 출처 주석이 **0개** (`test-conventions.md` 위반 — 전부 IMPL이면 리뷰 반려 대상인데, 그 표시조차 없음).

| 파일 | 프로덕션 결정 여부 | 조치 방향 |
|---|---|---|
| `OAuthReaderTest` | 없음 (repo 2줄 위임) | 5개 전부 삭제 대상. `find_success_found` 등은 스텁 값을 그대로 되받는 동어반복(#3) |
| `LocalAuthReaderTest` | `orElseThrow`만 | 7개 삭제 대상. design.md가 "결정 없음" 예시로 든 형태와 동일 |
| `TokenIssuerTest` | 거의 없음 | 6개 → 1~2개로 축소. `issue_with_different_member_roles`는 GUARDIAN만 씀(#10), `issue_unique_refresh_token_per_member`는 내가 스텁한 두 값이 다르다고 단언(#3), `issue_token_payload_contains_current_date`는 절대 실패 못하는 단언(#6) |
| `RefreshTokenWriterTest` | `save`의 만료시각 계산만 | 2개 → 1개 |
| `LocalAuthAuthenticatorTest` | 있음 (matches 분기) | 6개 → 4개 유지 가능하나 단언 보강 필요. `verifyPassword_success`는 **단언이 0개** |
| `LocalAuthManagerTest` | 있음 (중복검사 + 3단계 오케스트레이션) | **최우선 보강 대상.** `register_success`에 `given(...save(any())).willReturn(any())` — Mockito 오용(matcher를 반환값 자리에). `then(...).should().save(any(LocalAuth.class))` — 안티패턴 #1, email/password 뒤바뀌어도 통과. `termsAgreementWriter.register(memberKey)` 호출 검증 없음. `updatePassword_success`는 `findByMemberKey` 호출만 verify — `updatePassword()` 줄을 지워도 통과(#3) |
| `OAuthLinkValidatorTest` | 있음 (양쪽 분기) | 유지 |
| `OAuthManagerTest` | 있음 | 유지, InOrder 하나 추가 가치 있음 |
| `KakaoAuthenticatorTest` / `NaverAuthenticatorTest` | 있음 (null 분기, provider 매칭, 필드 매핑) | **테스트 0건 — 최대 공백.** 신규 작성 필요 |

### 구현 버그 (수정 안 함 — `/feature` 범위로 별도 보고)
- `NaverUser.toOAuthUser()` — `response`가 null이어도 그대로 접근. 네이버가 `resultcode != "00"`을 줄 때 `INVALID_OAUTH_USER`가 아니라 NPE. `KakaoUser`엔 null 가드가 있는데 여기만 없음.

---

## 2. `member` 신규 테스트 (이번 세션 작성분, `git status`상 `??`)

### 규칙 적용 확인 결과 — "리포지토리/엔티티 테스트가 규칙에 있는가?" (사용자 질문)

`test-design.md` §1 계층별 가치 표에 **명시돼 있음**:
- Entity ★★★ (상태 전이·불변식 — 가장 권장)
- Repository 조건부 (**직접 쓴 QueryDSL/JPQL만**, 파생 CRUD 메서드는 배제)

즉 두 계층 다 규칙에 반영돼 있고, 이번에 작성된 파일들은 아래처럼 대체로 이 조건을 지키고 있음.

| 파일 | 판정 | 근거 |
|---|---|---|
| `MemberTest.java` | 준수 | `updateProfile`의 null/blank 분기, `memberKey` 유일성, `SubscriptionType.BASIC` 기본값 — 전부 결정 지점. SPEC 주석 있음 |
| `MemberRepositoryTest.java` | 준수 | 테스트 대상 4개 메서드(`findAccount`, `findForUpdate`, `findByPhones`, `deleteByMemberKey`) + `MembersTermsAgreementRepositoryCustom.deleteByMemberKey` 전부 `*RepositoryCustom` QueryDSL 구현체. 파생 메서드(`findByMemberKey` 등)는 건드리지 않음. `@Tag("integration")` 있음 |
| `MyInfoResponseTest.java` | **부분 위반** | `MyInfoResponse.of()`의 null 분기(`member.getGender() == null → null`)가 테스트 목록에서 빠짐 — 실제 결정 지점 누락. 나머지(`combines_member_email_terms_and_safe_zones`)는 실제 객체로 필드별 단언, 준수 |
| `MemberControllerTest.java` (422줄) | 미검토 완료 — 다음 세션에서 이어서 | `test-design.md`상 Controller는 ★(로직 검증 아님, `@Valid`·직렬화·상태코드용)인데, 비밀번호 변경 성공/실패의 부수효과(재로그인 가능 여부)까지 이 계층에서 검증 중. 통합 테스트로서 정당화되는지(다계층 흐름 검증) vs business 계층 테스트와 중복인지 다음에 판단 필요 |

### 처리 완료 (auth 도메인, 2026-08-27)

**business**
- `LocalAuthServiceTest.signUp_success` — `register(any())` → ArgumentCaptor로 `phone`/`password`/`email` 필드별 단언
- `TokenRefreshServiceTest` — 예외 2건에 `never()` 반례 추가 (검증 실패 후 조회·삭제·재발급이 안 일어나는지)
- `OAuthServiceTest`, `CookieServiceTest` — 손대지 않음 (이미 값 고정·반례 검증 충족)

**implement**
- 삭제: `OAuthReaderTest`(5개), `LocalAuthReaderTest`(7개) — 결정 없는 순수 위임
- `LocalAuthManagerTest` 재작성 — Mockito 오용(`willReturn(any())`) 제거, `save(any())` → captor 필드별 단언,
  `termsAgreementWriter.register(memberKey)` 검증 추가, `changePassword`를 호출 verify → **엔티티 상태 단언**으로 교체
- `LocalAuthAuthenticatorTest` 재작성 — 동어반복 제거(고정 memberKey로 단언), 단언 0개였던 `verifyPassword_success`에
  `assertThatCode` 추가, 비밀번호 불일치 시 `memberReader` 미호출 반례 추가, 순수 위임 `encodePassword` 테스트 삭제
- `TokenIssuerTest` 6개 → 2개 (중복 3개 + 실패 불가 단언 1개 제거)
- `RefreshTokenWriterTest` 2개 → 1개 (`delete`는 순수 위임이라 삭제, `save`의 인자 자리바꿈 검증만 유지)
- `OAuthManagerTest`, `OAuthLinkValidatorTest`, `RefreshTokenReaderTest` — 로직 유지, 기댓값 출처 주석(`SPEC`) 보강

**member (Controller 통합 테스트 복원)**
- `MemberControllerTest` 재작성 (17개) — `AbstractIntegrationTest` 상속, `JwtGenerator`로 토큰 발급.
  GET /me(2), PATCH /me 프로필(5·경계값 20자 양쪽 포함), PATCH /me 비밀번호(3·트랜잭션 롤백 포함),
  POST /phones(3·403 포함), DELETE /me(3). 전 케이스가 상태코드 + **DB 재조회 부수효과**까지 단언

### auth 도메인 2차 — VO / business 나머지 / Controller (2026-08-27, 같은 날 이어서)

**implement (신규)**
- `KakaoAuthenticatorTest` 5개 신규 — `RestClient`를 `Answers.RETURNS_DEEP_STUBS` 목으로 두고 `KakaoUser`를 **그대로 반환**.
  URL·`Authorization` 헤더까지 값으로 고정해 `Bearer ` 누락이나 URL 변경이 스텁 미스로 드러나게 함.
  검증: id/email/nickname 매핑, `kakao_account` 부재, `profile` 부재, null 본문 → `INVALID_OAUTH_USER`, `supports()`
- `NaverAuthenticatorTest` 3개 신규 — 위와 동일 방식. `response` 필드 매핑, null 본문, `supports()`.
  `resultcode != "00"` 케이스는 아래 구현 버그 때문에 작성 불가 → 테스트 파일에 TODO로 남김

**business (나머지 2개 + 보강)**
- `OAuthServiceTest` 재작성 — `assertThat(result.accessToken()).isEqualTo(jwt.accessToken())` 동어반복(#3) 제거,
  고정 상수(`AuthFixture.ACCESS_TOKEN`)와 비교하도록 교체. 인라인 FQN(`org.mockito.ArgumentMatchers.*`) → import.
  provider 분기 반례(`naverAuthenticator` 미호출), 실패 시 후속 단계 미실행 반례 추가
- `CookieServiceTest` — 테스트가 없던 `create()` 추가 (ms→초 환산, HttpOnly/Secure/path). `extract`에 다른 쿠키를 섞어
  "첫 번째 쿠키"가 아니라 이름으로 고른다는 걸 검증하도록 보강
- `LocalAuthServiceTest`·`TokenRefreshServiceTest` — `times(0)` → `never()`, 기댓값 출처 주석 보강

**vo**
- `PasswordTest` — 중복이던 중간 길이 성공 케이스 삭제, **특수문자 포함 실패** 케이스 추가
  (길이·영숫자 조건을 모두 만족해 이 케이스가 없으면 정규식을 느슨하게 바꿔도 아무 테스트가 안 깨짐)
- `LocalEmailTest`·`EncodedPasswordTest` — 케이스 유지, 출처 주석만 보강

**controller (통합)**
- `AuthControllerTest` 7개 → 13개. 전 케이스가 에러코드 + DB 부수효과까지 단언.
  신규: 이메일 중복 가입, `/refresh` 성공·미보관 토큰·쿠키 없음, `/sign-out` 쿠키 없음, `/password` 성공·실패

### 이번에 발견한 구현/테스트 결함

1. **기존 `signOut` 통합 테스트 2건이 잘못된 저장소를 검증하고 있었다.**
   `RefreshToken`은 **PostgreSQL 엔티티**(`refresh_token` 테이블)인데 테스트는 Redis(`refresh:token:` 키)에 넣고
   Redis에서 지워졌는지 단언했다. 아무도 그 키를 지우지 않으므로 실행하면 실패한다.
   CI가 `integrationTest`를 돌리지 않아 드러나지 않은 것으로 보인다. → `RefreshTokenRepository` 기준으로 교체
2. **`SecurityConfig` permitAll 목록에 `/api/v1/auth/email`·`/api/v1/auth/password`가 빠져 있다.**
   `anyRequest().authenticated()`에 걸려 401. 이메일 찾기·비밀번호 재설정은 로그인할 수 없는 사용자가 쓰는 기능이라
   성립하지 않는다. `/feature` 범위 — 고칠 때까지 해당 통합 테스트 3건은 실패한다 (테스트 파일에 TODO로 명시)
3. `NaverUser.toOAuthUser()` — `response` null 가드 없음 (1차 감사에서 발견, 미해결)
4. `AuthController`에 `@ApiResponses` import가 남아 있다 (사용처는 없음). `forbidden.md` 금지 항목 — 정리 대상

### 남은 할 일 (우선순위)
1. 위 결함 2·3을 `/feature`로 수정 → 그 뒤 `./gradlew integrationTest` 로 auth·member 통합 테스트 실행 검증
   (현재는 notification 도메인 컴파일 오류로 전체 빌드가 막혀 있음 — auth/member 테스트 코드 자체는 컴파일 통과 확인)
2. `RefreshToken.isExpired()`가 무인자 `now()`라 만료 경계값(정각/±1초) 테스트 불가 —
   `Clock` 주입은 `/feature` 범위 (`test-design.md` §4). 해당 테스트에 TODO 주석으로 표시해둠
3. notification 도메인 정리 시 정책 위반 2건 함께 처리:
   `NotificationSettingControllerWebMvcTest`(Mock 기반 Controller 테스트 금지),
   `NotificationSettingRepositoryTest`(Repository 단독 테스트 금지)
4. ~~`MyInfoResponseTest`에 gender null 케이스 추가~~ — 해당 없음. Controller/Response 테스트 자체가 정책상 작성 대상 아님
5. ~~`MemberControllerTest`가 business 계층과 중복 검증하는지 판단~~ — 해당 없음. 삭제로 종결
