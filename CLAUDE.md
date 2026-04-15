# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 및 실행 명령어

```bash
# 로컬 인프라 실행 (PostgreSQL 16, Redis 7)
docker compose -f docker-compose-local.yml up -d

# 빌드
./gradlew build

# 단위 테스트 (integration 태그 제외)
./gradlew test

# 통합 테스트 (Testcontainers 사용)
./gradlew integrationTest

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.recaring.auth.business.LocalAuthServiceTest"

# QueryDSL Q클래스 재생성 (src/main/generated/)
./gradlew clean compileJava
```

## 아키텍처

**4계층 레이어드 아키텍처 (순방향 참조만 허용)**

```
Controller  →  Business(Service)  →  Implement  →  DataAccess(JPA/QueryDSL)
```

- **Business Layer**: 흐름 오케스트레이션만 담당. Repository 직접 참조 금지, 레이어 스킵 금지
- **Implement Layer**: Reader(조회), Writer(저장/수정/삭제), Manager(Reader+Writer 복합), Authenticator(인증), Validator(검증) 역할 분리. 동일 레이어 내 협력 허용

**도메인 패키지**

| 도메인 | 설명 |
|--------|------|
| `auth` | 로컬 로그인, OAuth2 (Kakao, Naver), 토큰 발급/갱신 |
| `care` | 케어 초대(`CareInvitation`) 및 관계(`CareRelationship`) 관리 |
| `member` | 회원 프로필, 전화번호 인증 결과 연동 |
| `sms` | CoolSMS 기반 전화번호 인증 코드 발송/검증 |
| `security` | JWT 필터, 핸들러, `JwtGenerator`, `JwtValidator` |
| `support` | `AppException`, `ErrorType`, `ErrorCode`, `ApiResponse` |
| `common` | AOP, `BaseEntity`, 글로벌 예외 핸들러 |
| `config` | Security, Redis, Mapper, Swagger 설정 |

**Care 도메인 핵심 모델**
- `CareRole`: `GUARDIAN`(보호자), `MANAGER`(관계자)
- `CareInvitationStatus`: `PENDING` → `ACCEPTED` / `REJECTED`
- 회원 역할: `GUARDIAN`, `WARD`(보호 대상자) — 역할에 따라 일부 엔드포인트 접근 제한

## 핵심 패턴

**인증 흐름**
```
Authorization: Bearer {accessToken}
→ JwtAuthenticationFilter (memberKey를 SecurityContext에 저장)
→ @AuthMember String memberKey (Controller 파라미터 주입)
```
Access Token은 응답 바디, Refresh Token은 HttpOnly Cookie로 전달.

**Request → Command 패턴**
```java
// Controller에서 toCommand()로 변환 후 Service 호출
localAuthService.signUp(request.toCommand());
```
`toCommand()` 내부에서 VO 생성 및 유효성 검증 (`AppException(ErrorType.xxx)` throw).

**논리 삭제** — Entity에 `@SQLDelete` + `@SQLRestriction("deleted_at IS NULL")` 적용.  
삭제 시 `entity.delete()` 호출, `memberRepository.delete()` 직접 호출 금지.

**외부 식별자** — `memberKey` (UUID) 사용, DB PK(`id`) 외부 노출 절대 금지.  
`CareInvitation`은 `requestKey` (UUID) 사용.

**에러 코드 범위**

| 범위 | 도메인 |
|------|--------|
| `E400`, `E401`, `E403`, `E404`, `E429`, `E500` | 공통 |
| `E2xxx` | Auth (JWT, OAuth, 로컬 인증) |
| `E3xxx` | Member |
| `E4xxx` | SMS / Phone Verification |
| `E5xxx` | Care (E5010까지 사용 중) |

## 테스트 구조

| 종류 | 기반 클래스 | 특징 |
|------|------------|------|
| 단위 | `@ExtendWith(MockitoExtension.class)` | BDD 스타일 `given/when/then`, `@DisplayName` 한국어 |
| 통합 | `AbstractIntegrationTest` | `@Tag("integration")` 필수, `RestTestClient` 사용, Testcontainers (postgres:16, redis:7) |
| Repository | `AbstractRepositoryTest` | `@DataJpaTest` + Testcontainers PostgreSQL, `TestEntityManager` |

테스트 데이터는 `XxxFixture` 클래스에 static 상수 및 팩토리 메서드로 정의.

## Git 컨벤션

- 브랜치: `feature/{N}`, `hotfix/{N}`
- 커밋: `{type}[#{N}]: 설명` — type: `feature`, `fix`, `hotfix`, `refactor`, `style`, `docs`, `test`, `chore`
