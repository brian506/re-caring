# ErrorType / ErrorCode 패턴

## 구조

`ErrorCode` (코드 식별자 enum) + `ErrorType` (HttpStatus + 메시지 + LogLevel 포함 enum) 이원화.

```java
// ErrorType 추가 예시
CARE_INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, ErrorCode.E5005, "만료된 케어 요청입니다.", LogLevel.WARN),

// AppException throw
throw new AppException(ErrorType.CARE_INVITATION_EXPIRED);
```

## 도메인별 에러 코드 범위

| 범위 | 도메인 |
|------|--------|
| `E400`, `E401`, `E403`, `E404`, `E429`, `E500` | 공통 |
| `E2000` ~ `E2xxx` | Auth (JWT, OAuth, 로컬 인증) |
| `E3000` ~ `E3xxx` | Member |
| `E4000` ~ `E4xxx` | SMS / Phone Verification |
| `E5000` ~ `E5010` | Care (현재까지 사용) |

## 새 에러 추가 절차

1. `ErrorCode.java` 에서 해당 도메인 범위의 다음 번호 확인
2. `ErrorCode` enum에 새 코드 추가 (예: `E5011`)
3. `ErrorType` enum에 추가:

```java
NEW_ERROR_TYPE(HttpStatus.BAD_REQUEST, ErrorCode.E5011, "에러 메시지", LogLevel.WARN),
```

LogLevel 선택:
- `INFO`: 정상 흐름의 일부 (예: 로그인 실패)
- `WARN`: 비즈니스 규칙 위반 (예: 중복, 한도 초과)
- `ERROR`: 예상치 못한 시스템 오류
