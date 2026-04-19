# 구현 패턴

## Command 사용 판단

### Command를 쓰지 않는 경우

**1) 필드 3개 이하 — Request 필드를 Service에 직접 전달**

```java
// Request
public record AddWardRequest(@NotBlank String phoneNumber) {}

// Controller
careInvitationService.sendWardInvitation(memberKey, request.phoneNumber());

// Service
public void sendWardInvitation(String memberKey, String phoneNumber) { ... }
```

**2) VO 하나로 표현 가능한 경우 — VO를 Service에 직접 전달**

```java
// Request
public record SendCodeRequest(@NotBlank String phone) {}

// Controller
smsService.sendCode(new PhoneNumber(request.phone()));

// Service
public void sendCode(PhoneNumber phoneNumber) { ... }
```

### Command를 쓰는 경우

**필드 4개 이상이거나, 여러 VO 변환이 한 번에 필요한 경우**

```java
// Request — toCommand() 내부에서 VO 생성 + AppException 검증
public record SignUpRequest(
        String verificationToken,
        String email,       // → LocalEmail VO
        String password,    // → Password VO
        String name,
        LocalDate birth,
        Gender gender,
        MemberRole role,
        Boolean isTermsAgreed,
        Boolean isPrivacyAgreed
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(
                verificationToken,
                new LocalEmail(email),   // VO 생성자에서 AppException throw
                new Password(password),
                name, birth, gender, role
        );
    }
}

// Command — VO가 포함된 불변 객체
public record SignUpCommand(
        String verificationToken,
        LocalEmail email,
        Password password,
        String name,
        LocalDate birth,
        Gender gender,
        MemberRole role
) {}

// Controller
localAuthService.signUp(request.toCommand());
```

### 판단 요약

| 상황 | 방식 |
|------|------|
| 조회 파라미터 1~2개 (id, key 등) | 직접 파라미터 |
| 단일 값 형식 검증 필요 (전화번호, 이메일) | VO 직접 |
| 필드 3개 이하, 단순 전달 | Request 필드 직접 |
| 필드 4개 이상 또는 VO 복수 변환 | Command + toCommand() |

## VO 패턴

- VO 생성자에서 AppException throw (Validation은 VO 내부 책임)
- `record` 사용, compact constructor로 검증

```java
public record PhoneNumber(String value) {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");

    public PhoneNumber {
        if (value == null || value.isBlank()) throw new AppException(ErrorType.INVALID_PHONE_FORMAT);
        if (!PHONE_PATTERN.matcher(value).matches()) throw new AppException(ErrorType.INVALID_PHONE_FORMAT);
    }
}
```

## Request 패턴

- `record` 사용
- Bean Validation 어노테이션 (`@NotBlank`, `@Email`, `@NotNull` 등) 으로 1차 검증
- 복잡한 비즈니스 검증은 VO 생성자 또는 Validator에서 처리

## ApiResponse 반환

```java
// 데이터 있음
return ResponseEntity.ok(ApiResponse.success(responseDto));

// 데이터 없음 (201 Created 등)
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
```

## CareRole / MemberRole

- `CareRole`: `GUARDIAN`(보호자), `MANAGER`(관계자)
- `MemberRole`: `GUARDIAN`, `WARD`(보호 대상자)
- 일부 엔드포인트는 역할에 따라 접근 제한 → Security 설정 확인 필요
