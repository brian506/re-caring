# CLAUDE.md
이 파일은 Claude가 이 프로젝트 코드베이스를 작업할 때 참고하는 가이드입니다.

---

## 프로젝트 개요

- **아키텍처**: 4계층 레이어드 아키텍처 (Domain-Driven Design 기반)
- **기술 스택**: Spring Boot 4.0.3, Java 21, PostgreSQL 16, Redis 7, QueryDSL 7.0, jjwt 0.12.6, CoolSMS (net.nurigo:sdk:4.3.0)
- **핵심 원칙**: 비즈니스 로직의 가시화, 레이어 간 명확한 책임 분리, 기술 의존성 격리

---

## Git 컨벤션

### Branch 전략

```
master       ─── 운영 배포 브랜치
  └─ develop ─── 개발 통합 브랜치
       ├─ feature/{IssueNumber}  예) feature/1, feature/12
       └─ hotfix/{IssueNumber}   예) hotfix/1, hotfix/12
```

### Commit 메시지 형식

```
{type}[#{IssueNumber}]: {description}
예) feature[#12]: OOO 기능 구현
    fix[#34]: 로그인 버그 수정
```

| Type | 설명 |
|------|------|
| `feature` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 |
| `style` | 코드 포맷 변경 (로직 수정 없는 경우) |
| `docs` | 문서 작성/수정 |
| `test` | 테스트 코드 작성/변경 |
| `hotfix` | 긴급 수정 (치명적인 버그) |
| `chore` | 빌드 업무 수정 / 패키지 구성 업데이트 |

---

## 아키텍처: 4계층 레이어

### 레이어 구조

```
Controller Layer   ← 외부 요청/응답, Request DTO → Command 변환, Service 호출
      ↓
Business Layer     ← 비즈니스 흐름 오케스트레이션, Service
      ↓
Implement Layer    ← 상세 구현 로직, Reader / Writer / Manager / Authenticator
      ↓
Data Access Layer  ← JPA Repository, QueryDSL, 기술 의존성 격리
```

### 레이어 규칙 (4가지 핵심 규칙)

1. **순방향 참조만 허용**: 레이어는 위에서 아래 방향으로만 참조한다.
2. **역방향 참조 금지**: 하위 레이어는 상위 레이어를 알지 못한다.
3. **레이어 스킵 금지**: Business Layer가 Data Access Layer(Repository)를 직접 참조하지 않는다.
4. **동일 레이어 간 참조 금지**: 단, Implement Layer 내부 클래스 간 협력은 예외적으로 허용한다.

---

## 패키지 구조

도메인 주도 설계에 따라 기능(feature)이 아닌 **도메인(domain)** 중심으로 패키지를 구성한다.

```
com.recaring
├── auth/                      # 인증 도메인 (로컬 로그인, OAuth2)
│   ├── business/              # Service (비즈니스 흐름 조합)
│   │   └── command/           # Command 객체 (record)
│   ├── controller/            # REST Controller
│   │   ├── request/           # Request DTO (record, toCommand() 포함)
│   │   └── response/          # Response DTO (record)
│   ├── dataaccess/            # Data Access Layer
│   │   ├── entity/            # JPA Entity
│   │   └── repository/        # Spring Data JPA Repository
│   │       └── custom/        # QueryDSL 커스텀 Repository
│   ├── implement/             # Reader / Writer / Manager / Authenticator
│   │   ├── local/             # 로컬 인증 구현체
│   │   └── oauth/             # OAuth 인증 구현체
│   └── vo/                    # Value Object (record, 자체 유효성 검증)
│       ├── kakao/             # Kakao 관련 VO
│       └── naver/             # Naver 관련 VO
├── common/                    # 공통 유틸, AOP, 글로벌 핸들러
│   ├── aspect/                # AOP Aspect
│   ├── controller/            # GlobalExceptionHandler (ApiControllerAdvice)
│   ├── entity/                # BaseEntity
│   ├── enums/                 # 공통 Enum
│   ├── mapper/                # Entity ↔ VO 변환 Mapper
│   └── utils/                 # 유틸리티 클래스
├── config/                    # 설정
│   ├── auth/                  # Security, JWT 설정
│   └── infra/                 # Redis, CoolSMS 설정
├── domain/                    # 비즈니스 도메인
│   └── member/                # 회원 도메인
│       ├── controller/        # MemberController
│       ├── dataaccess/        # Member Entity, Repository
│       └── implement/         # MemberReader, MemberWriter
├── security/                  # JWT 필터, 핸들러
│   ├── filter/
│   ├── handler/
│   ├── jwt/                   # JwtGenerator, JwtValidator
│   └── vo/                    # AuthMember, Jwt, TokenPayload
├── sms/                       # 전화번호 인증 도메인
│   ├── business/
│   ├── controller/
│   ├── implement/
│   └── vo/
└── support/                   # 공통 지원
    ├── exception/             # AppException, ErrorType, ErrorCode
    ├── repository/            # QueryDSL 공통 지원
    └── response/              # ApiResponse, ResultType
```

---

## 명명 규칙

### 클래스 네이밍

| 종류 | 패턴 | 예시 |
|------|------|------|
| Service | `XxxService` | `LocalAuthService`, `PhoneVerificationService` |
| Controller | `XxxController` | `AuthController`, `MemberController` |
| Request DTO | `XxxRequest` | `SignUpRequest`, `SignInRequest` |
| Response DTO | `XxxResponse` | `SignInResponse`, `MaskEmailResponse` |
| Command | `XxxCommand` | `SignUpCommand`, `SignInCommand` |
| Reader | `XxxReader` | `MemberReader`, `LocalAuthReader` |
| Writer | `XxxWriter` | `MemberWriter`, `RefreshTokenWriter` |
| Manager | `XxxManager` | `LocalAuthManager`, `OAuthManager` |
| Authenticator | `XxxAuthenticator` | `LocalAuthAuthenticator`, `KakaoAuthenticator` |
| Value Object | 의미있는 이름 | `Password`, `LocalEmail`, `PhoneNumber` |
| Entity | 단수 명사 | `Member`, `LocalAuth`, `OAuth` |
| Repository | `XxxRepository` | `MemberRepository`, `LocalAuthRepository` |
| QueryDSL 인터페이스 | `XxxRepositoryCustom` | `OAuthRepositoryCustom` |
| QueryDSL 구현체 | `XxxRepositoryCustomImpl` | `OAuthRepositoryCustomImpl` |
| Fixture (테스트) | `XxxFixture` | `MemberFixture`, `AuthFixture` |

### Implement Layer 역할 구분

| 컴포넌트 | 역할 | 예시 |
|---------|------|------|
| `Reader` | 조회 전용 구현체 | `MemberReader`, `LocalAuthReader` |
| `Writer` | 저장/수정/삭제 구현체 | `MemberWriter`, `RefreshTokenWriter` |
| `Manager` | 조회·쓰기를 복합 조합하는 구현체 | `LocalAuthManager`, `OAuthManager` |
| `Authenticator` | 인증/인가 전담 구현체 | `LocalAuthAuthenticator`, `KakaoAuthenticator` |

### CRUD 메서드명

| 기능 | Implement Layer | Repository (JPA) |
|------|----------------|-----------------|
| 단일 조회 | `find` | `findBy~` |
| 다중 조회 | `findAll` | `findAll~` |
| 생성 | `register` / `add` | `save` |
| 수정 | `update` | (dirty checking) |
| 삭제 | `remove` | `delete` |

---

## 코드 작성 원칙

### Controller Layer

Request DTO를 받아 `toCommand()`로 Command로 변환 후 Service를 호출한다. HTTP 관련 처리(쿠키, 헤더)는 Controller에서 담당한다.

```java
@PostMapping("/sign-up")
public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody SignUpRequest request) {
    localAuthService.signUp(request.toCommand());
    return ResponseEntity.ok(ApiResponse.success());
}

@PostMapping("/sign-in/local")
public ResponseEntity<ApiResponse<SignInResponse>> signInByLocal(
        @Valid @RequestBody SignInRequest request,
        HttpServletResponse response
) {
    Jwt jwt = localAuthService.signIn(request.toCommand());
    response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(jwt.refreshToken()).toString());
    return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
}
```

### 인증이 필요한 엔드포인트 — @AuthMember

로그인한 사용자의 `memberKey`가 필요한 엔드포인트에서는 `@AuthMember String memberKey` 파라미터로 주입받는다. 엔티티를 컨트롤러에서 직접 받지 않는다. 실제 `Member` 조회가 필요한 경우 서비스 계층에서 `MemberReader`를 통해 한다.

**인증 흐름**

```
Authorization: Bearer {accessToken}
      ↓
JwtAuthenticationFilter  →  JWT 파싱 후 memberKey(String)를 SecurityContext에 저장
      ↓
@AuthMember              →  SecurityContext의 principal(memberKey) 주입
      ↓
Controller 파라미터로 주입
```

**사용 방법**

```java
// memberKey만 주입받고, Member 조회는 서비스에서 처리
@GetMapping("/profile")
public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
        @AuthMember String memberKey
) {
    return ResponseEntity.ok(ApiResponse.success(memberService.getProfile(memberKey)));
}

@PatchMapping("/nickname")
public ResponseEntity<ApiResponse<Void>> updateNickname(
        @AuthMember String memberKey,
        @Valid @RequestBody UpdateNicknameRequest request
) {
    memberService.updateNickname(memberKey, request.toCommand());
    return ResponseEntity.ok(ApiResponse.success());
}
```

**`@AuthMember` 어노테이션 정의**

```java
// security/vo/AuthMember.java
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface AuthMember {}
```

> `JwtAuthenticationFilter`에서 JWT를 파싱해 `memberKey`(String)를 principal로 SecurityContext에 저장한다.
> `SecurityConfig`에서 인증이 필요한 경로는 `.anyRequest().authenticated()`로 보호되어 있으며,
> 토큰 없이 접근 시 `JwtAuthenticationEntryPoint`가 401을 반환한다.

### Business Layer (Service)

Service는 **비즈니스 흐름의 오케스트레이터**다. 상세 구현 로직을 갖지 않는다. Repository를 직접 주입받지 않으며, 반드시 Implement Layer를 통해서만 데이터에 접근한다.

```java
// ✅ 좋은 예: 비즈니스 흐름이 읽힌다
@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final TokenIssuer tokenIssuer;
    private final LocalAuthAuthenticator authAuthenticator;
    private final MemberReader memberReader;
    private final LocalAuthManager localAuthManager;
    private final PhoneVerificationReader phoneVerificationReader;

    public void signUp(SignUpCommand command) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(command.smsToken());
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(command.password());
        localAuthManager.register(command.toNewLocalMember(phone, encodedPassword));
    }

    public Jwt signIn(SignInCommand command) {
        Member member = authAuthenticator.authenticate(command);
        return tokenIssuer.issue(member);
    }
}

// ❌ 나쁜 예: Service가 Repository를 직접 들고 상세 구현을 처리
public void signUp(SignUpCommand command) {
    if (localAuthRepository.existsByEmail(command.email().value())) { // Repository 직접 참조 금지
        throw new AppException(ErrorType.INVALID_EMAIL);
    }
    // ... 검증, 저장 로직이 Service에 뒤섞임
}
```

### Implement Layer

각 클래스는 **단 하나의 책임**만 가진다. JPA Repository를 직접 사용하고 영속성 로직을 캡슐화한다.

```java
// ✅ Reader 예시: 조회만 담당
@Component
@RequiredArgsConstructor
public class MemberReader {
    private final MemberRepository memberRepository;

    public Member findByMemberKey(String memberKey) {
        return memberRepository.findByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }
}

// ✅ Writer 예시: 저장/수정/삭제만 담당
@Component
@RequiredArgsConstructor
public class MemberWriter {
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Transactional
    public String registerLocalMember(NewLocalMember newLocalMember) {
        Member member = memberMapper.toLocalMember(newLocalMember);
        return memberRepository.save(member).getMemberKey();
    }
}

// ✅ Manager 예시: 조회·쓰기를 복합 조합하는 비즈니스 로직 담당
@Component
@RequiredArgsConstructor
public class LocalAuthManager {
    private final LocalAuthReader localAuthReader;
    private final LocalAuthRepository localAuthRepository;
    private final MemberWriter memberWriter;

    @Transactional
    public void register(NewLocalMember member) {
        if (localAuthRepository.existsByEmail(member.email().value())) {
            throw new AppException(ErrorType.INVALID_EMAIL);
        }
        String memberKey = memberWriter.registerLocalMember(member);
        localAuthRepository.save(mapper.createLocalAuth(memberKey, member.email().value(), member.password().value()));
    }
}
```

### Value Object (VO)

VO는 **Java record**로 작성하며, compact constructor에서 유효성을 검증한다. 검증 실패 시 `AppException(ErrorType.xxx)` throw.

```java
public record LocalEmail(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public LocalEmail {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorType.EMAIL_IS_NULL);
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new AppException(ErrorType.INVALID_EMAIL_FORMAT);
        }
    }
}
```

### Request / Command 패턴

- `XxxRequest`: Controller 진입점, `@Valid` Bean Validation 적용
- `toCommand()` 메서드에서 VO 생성 및 Command 변환 (검증이 Controller 레이어에서 일어남)

```java
public record SignUpRequest(
        @NotBlank String verificationToken,
        @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull LocalDate birth,
        @NotNull Gender gender,
        @NotNull MemberRole role
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(
                verificationToken,
                new LocalEmail(email),    // VO로 변환 → 유효성 검증 발생
                new Password(password),   // VO로 변환 → 유효성 검증 발생
                name, birth, gender, role
        );
    }
}
```

---

## Entity 작성 규칙

- `BaseEntity` 상속 (`createdAt`, `updatedAt`, `deletedAt` 자동 관리)
- `@Setter` 사용 금지, 의미 있는 메서드로 상태 변경 표현
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수
- `@Builder`는 `public` 생성자에 적용
- DB PK(`id`)와 별도로 **비즈니스 식별자** `memberKey` (UUID) 사용 — 외부 노출용
- `@Column(name = "xxx_id")` 으로 컬럼명 명시

```java
@Getter
@Entity
@Table(name = "members")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String memberKey;  // UUID, 외부 노출용 식별자. DB PK는 절대 노출 금지.

    // ... 필드들

    @Builder
    public Member(String phone, String name, LocalDate birth, Gender gender, MemberRole role, SignUpType signUpType) {
        this.memberKey = UUID.randomUUID().toString();
        // ...
    }
}
```

### Soft Delete

물리 삭제(DB row 제거) 대신 **논리 삭제(삭제 플래그 처리)**를 기본 전략으로 사용한다.

- `BaseEntity`에 `deletedAt` 필드 존재 → `null`이면 활성, 값이 있으면 삭제됨
- Entity에 `@SQLRestriction("deleted_at IS NULL")` 적용 → JPA 조회 시 자동으로 필터링
- `entity.delete()` 호출 → dirty checking으로 자동 반영 (물리 삭제 금지)

```java
// ✅ Writer에서 논리 삭제
public void remove(Member member) {
    member.delete();  // deletedAt 세팅, dirty checking으로 자동 반영
}

// ❌ 물리 삭제 금지
public void remove(Member member) {
    memberRepository.delete(member);
}
```

삭제된 데이터를 포함해 조회해야 하는 경우 — 별도 쿼리 메서드로 의도를 명시한다.

```java
// 삭제 포함 전체 조회: 명시적으로 네이밍하여 의도를 드러냄
@Query("SELECT m FROM Member m WHERE m.id = :id")
Optional<Member> findByIdIncludingDeleted(@Param("id") Long id);
```

---

## 에러 처리

### ErrorType 정의 방식

`ErrorType` enum에 `(HttpStatus, ErrorCode, message, LogLevel)` 조합으로 정의한다.

```java
NOT_FOUND_ACCOUNT(HttpStatus.BAD_REQUEST, ErrorCode.E3016, "존재하지 않는 계정 정보입니다.", LogLevel.WARN),
```

- 클라이언트 오류(4xx): `LogLevel.WARN`
- 서버 오류(5xx): `LogLevel.ERROR`

### ErrorCode 네이밍

- 공통: `E400`, `E401`, `E403`, `E404`, `E429`, `E500`
- 도메인별: `E3000`번대부터 순번으로 부여 (현재 `E3026`까지 사용됨)

### AppException 사용

모든 비즈니스 예외는 도메인별 Exception을 만들지 않고 `AppException`으로 통일한다.

```java
throw new AppException(ErrorType.NOT_FOUND_ACCOUNT);
throw new AppException(ErrorType.NOT_FOUND_ACCOUNT, extraData);  // 디버그 데이터 포함 시
```

---

## API 응답 포맷

모든 API 응답은 `ApiResponse<T>` 래핑을 사용한다.

```java
// 성공 (데이터 없음)
return ResponseEntity.ok(ApiResponse.success());

// 성공 (데이터 포함)
return ResponseEntity.ok(ApiResponse.success(new SignInResponse(jwt.accessToken())));
```

Controller 반환 타입: `ResponseEntity<ApiResponse<T>>`

---

## API URL 패턴

```
/api/v1/{domain}/{action}
```

예시:
- `POST /api/v1/auth/sign-up`
- `POST /api/v1/auth/sign-in/local`
- `POST /api/v1/auth/sign-in/kakao`
- `POST /api/v1/auth/phone/send-code`

---

## 보안 / 인증

- **Access Token**: JWT, 응답 바디로 전달
- **Refresh Token**: JWT, HttpOnly Cookie로 전달
- **OAuth2**: Kakao, Naver 지원
- **외부 노출 식별자**: `memberKey` (UUID) 사용, DB PK(`id`) 절대 미노출

---

## Lombok 사용 규칙

- DI: `@RequiredArgsConstructor` (생성자 주입)
- Entity: `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- VO / Command / Request / Response: **record** 사용 (Lombok 불필요)

---

## 테스트 작성 규칙

### 단위 테스트

- `@ExtendWith(MockitoExtension.class)`
- Mockito BDD 스타일: `given / when / then`
- `@DisplayName`에 한국어로 명확한 설명 작성

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService 단위 테스트")
class LocalAuthServiceTest {

    @InjectMocks
    private LocalAuthService localAuthService;

    @Mock
    private TokenIssuer tokenIssuer;
    @Mock
    private LocalAuthAuthenticator authAuthenticator;

    @Test
    @DisplayName("로그인 시 TokenIssuer를 통해 JWT가 발급된다")
    void signIn_success() {
        // given
        given(authAuthenticator.authenticate(command)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(expectedJwt);

        // when
        Jwt result = localAuthService.signIn(command);

        // then
        assertThat(result.accessToken()).isEqualTo(AuthFixture.ACCESS_TOKEN);
    }
}
```

### 통합 테스트

- `AbstractIntegrationTest` 상속
- Testcontainers로 PostgreSQL 16, Redis 7 실제 컨테이너 사용
- `@Tag("integration")` 필수 (기본 테스트와 분리 실행)
- `RestTestClient` 사용

### Repository 테스트

- `AbstractRepositoryTest` 상속
- `@DataJpaTest` + Testcontainers PostgreSQL
- `TestEntityManager` 사용, `@BeforeEach`에서 `flush/clear`

### Fixture 클래스

테스트 데이터는 `XxxFixture` 클래스에 static 메서드로 정의한다. 상수와 팩토리 메서드를 분리한다.

```java
public class MemberFixture {
    public static final String NAME = "홍길동";
    public static final LocalDate BIRTH = LocalDate.of(1990, 1, 1);

    public static Member createMember() { ... }
    public static SignUpCommand createSignUpCommand() { ... }
}
```

### 테스트 실행

```bash
# 단위 테스트
./gradlew test

# 통합 테스트
./gradlew integrationTest
```

---

## 로컬 개발 환경

```bash
# 로컬 인프라 실행 (PostgreSQL 16, Redis 7)
docker compose -f docker-compose-local.yml up -d

# 개발 서버 실행
docker compose -f docker-compose-dev.yml up -d

# 빌드
./gradlew build
```

---

## 의존성 원칙

- Business Layer는 JPA, 외부 라이브러리 등 **기술 구현체를 직접 의존하지 않는다**.
- Repository는 반드시 Implement Layer에서만 사용한다.
- 기술 변경 시 Implement / Data Access Layer만 수정되도록 설계한다.

---

## 표준에서 벗어난 결정사항

이 섹션은 팀이 표준 레이어 또는 컨벤션을 변경/확장한 경우 그 이유와 함께 기록한다.

| 날짜 | 변경 내용 | 이유 |
|------|----------|------|
| (예시) 2026-03-10 | `XxxFacade` 레이어 추가 | 두 서비스 간 조합이 필요한 복잡한 비즈니스 흐름 처리 |

---

> **핵심 목표**: 상세 구현 로직을 몰라도 비즈니스의 흐름은 이해 가능한 코드를 작성한다.
