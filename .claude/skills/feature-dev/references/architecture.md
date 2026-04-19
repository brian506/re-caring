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
| `member` | 회원 프로필, 전화번호 인증 연동 |
| `sms` | CoolSMS 기반 인증코드 발송/검증 |
| `security` | JWT 필터, JwtGenerator, JwtValidator |
| `support` | AppException, ErrorType, ErrorCode, ApiResponse |
| `common` | AOP, BaseEntity, 글로벌 예외 핸들러 |
| `config` | Security, Redis, Mapper, Swagger 설정 |

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

## 인증 흐름

```
Authorization: Bearer {accessToken}
→ JwtAuthenticationFilter (memberKey를 SecurityContext에 저장)
→ @AuthMember String memberKey (Controller 파라미터 주입)
```

Access Token → 응답 바디, Refresh Token → HttpOnly Cookie

## 논리 삭제

```java
@SQLDelete(sql = "UPDATE table SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Entity extends BaseEntity { ... }
```

- 삭제 시 `entity.delete()` 호출
- `repository.delete()` 직접 호출 금지

## 외부 식별자

- 회원: `memberKey` (UUID) — DB PK 외부 노출 절대 금지
- CareInvitation: `requestKey` (UUID)
