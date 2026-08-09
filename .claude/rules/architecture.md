# 아키텍처 규칙

## 계층 구조 (순방향 참조만 허용)

```
Controller → Business(Service) → Implement → DataAccess(JPA/QueryDSL)
```

- Business는 Repository 직접 참조 금지, 레이어 스킵 금지
- Implement 계층 내 협력은 허용 (예: Manager가 Reader + Writer 호출)

## 도메인 패키지

| 도메인 | 설명 |
|--------|------|
| `auth` | 로컬/OAuth2 인증, 토큰 발급 |
| `care` | 케어 초대(CareInvitation), 케어 관계(CareRelationship) |
| `device` | Device Token 발급·검증 (GPS 전용 장기 인증) |
| `member` | 회원 프로필, 전화번호 인증 연동 |
| `sms` | CoolSMS 기반 인증코드 발송/검증 |
| `security` | JWT 필터, DeviceTokenAuthFilter, JwtGenerator, JwtValidator |
| `support` | AppException, ErrorType, ErrorCode, ApiResponse |
| `common` | AOP, BaseEntity, 글로벌 예외 핸들러 |
| `config` | Security, Redis, Mapper, Swagger 설정 |

## 도메인 내부 패키지 구조

```
{domain}/
├── business/       # Service — 유즈케이스 오케스트레이션
├── controller/     # REST Controller, Request/Response
│   ├── request/
│   └── response/
├── dataaccess/     # Entity, Repository
│   ├── entity/
│   └── repository/
├── implement/      # Reader, Writer, Manager, Validator, ...
└── vo/             # 도메인 객체 (불변 record) — 레이어 간 흐름 객체
```

`implement/`의 클래스가 많아지면 관심사별 하위 패키지로 나눈다. 도메인 전체에서 쓰는 공용 클래스만 `implement/` 바로 아래 둔다.

```
location/implement/
├── gps/            # GpsHistoryManager, GpsLatestCacheManager, GpsLatestCacheListener
├── detection/      # DetectionPublisher, DetectionListener, DetectionResultConsumer, ...
├── sse/            # SseEmitterManager
├── setting/        # LocationSettingManager
└── LocationValidator.java, CareRelationshipCacheReader.java   # 공용
```

`vo/`는 해당 도메인의 핵심 성질을 담는 불변 record다. business DTO가 아니라 도메인 개념 자체를 표현한다.
entity → VO 변환은 VO의 `from()` 팩토리 메서드에서 담당하며, implement 계층(Reader 등)이 변환해 반환한다.

## Implement 계층 역할 분리

| 역할 | 사용 시점 | 예시 |
|------|----------|------|
| **Reader** | 조회 전용 | `CareRelationshipReader`, `MemberReader` |
| **Writer** | 저장·수정·삭제 | `CareInvitationWriter`, `MemberWriter` |
| **Manager** | Reader + Writer 복합 오케스트레이션, @Transactional | `CareInvitationManager` |
| **Authenticator** | 인증 처리 (비밀번호 검증·인코딩) | `LocalAuthAuthenticator` |
| **Validator** | 비즈니스 규칙 검증, AppException throw | `CareRelationshipValidator` |

**역할 선택 기준:**
- 단순 조회만 → Reader
- 단순 저장/수정/삭제만 → Writer
- 조회 + 저장을 하나의 트랜잭션으로 묶어야 할 때 → Manager
- 입력값이 아닌 비즈니스 규칙(중복, 권한, 한도) 검증 → Validator
- 인증 관련 처리 → Authenticator

**과분리 금지:** 같은 저장소를 다루는 Reader와 Writer가 각각 메서드 하나뿐이면 나누지 말고 Manager 하나로 합친다.
역할 이름을 붙이는 것 자체가 목적이 아니다. 클래스가 늘어나 추적만 어려워진다.

```
DeviceStateReader(find) + DeviceStateWriter(save)  →  DeviceStateManager(find, save)
```

Reader/Writer 분리는 메서드가 여러 개로 늘어 각각의 책임이 뚜렷해질 때 하면 된다.

## 인증 흐름

### JWT (보호자/일반 API)
```
Authorization: Bearer {accessToken}
→ JwtAuthenticationFilter (memberKey를 SecurityContext에 저장)
→ @AuthMember String memberKey (Controller 파라미터 주입)
```
Access Token → 응답 바디, Refresh Token → HttpOnly Cookie

### Device Token (GPS 전용, WARD 백그라운드 앱)
```
Authorization: Device {deviceToken}
→ DeviceTokenAuthFilter (wardKey를 SecurityContext에 저장)
→ @AuthMember String memberKey (기존 LocationController 그대로 사용)
```
- `POST /api/v1/location/gps` 경로에서만 작동
- JwtAuthenticationFilter는 이 경로를 건너뜀 (shouldNotFilter)
- 발급: `POST /api/v1/device/token` (JWT 인증, WARD 전용, 최초 1회)
- 재발급: 동일 엔드포인트 재호출 → 기존 토큰 교체

## 삭제 정책

모든 삭제는 **hard-delete**로 통일한다. soft-delete(`@SQLDelete`, `@SQLRestriction`, `deleted_at`)를 사용하지 않는다.

- 단건 삭제: `repository.delete(entity)`
- 조건 삭제: 파생 메서드 `deleteBy...` 또는 `@Modifying` 벌크 쿼리
- 여러 테이블을 한 번에 정리해야 하면 Manager(@Transactional)에서 오케스트레이션

## 외부 식별자

- 회원: `memberKey` (UUID) — DB PK 외부 노출 절대 금지
- CareInvitation: `requestKey` (UUID)

## 중첩 타입 금지

클래스·record·interface 안에 또 다른 클래스·record·enum·interface를 선언하지 않는다.

```java
// 금지
public class FooRequest {
    public record Bar(String value) {} // 내부 record 금지
    public enum Status { ACTIVE, INACTIVE } // 내부 enum 금지
}

// 허용 — 별도 파일로 분리
// FooRequest.java, FooStatus.java 각각 독립 파일
```

- 모든 타입은 독립된 파일로 분리한다
- enum도 별도 파일(`{도메인}/{패키지}/{Name}.java`)로 선언한다

## 인덱스 규칙

`@Table(indexes = {...})` 금지. 인덱스는 별도 DDL로 관리하며 Entity에 TODO 주석으로 표시한다 (CLAUDE.md 참고).

---

> 현재 API·엔티티·기술 부채 목록은 `snapshot.md` 참고.
