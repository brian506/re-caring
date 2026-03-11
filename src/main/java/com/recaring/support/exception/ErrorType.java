package com.recaring.support.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    REQUIRED_AUTH(HttpStatus.UNAUTHORIZED, ErrorCode.E401, "리소스에 접근하기 위한 인증이 필요합니다.", LogLevel.WARN),
    FAILED_AUTH(HttpStatus.BAD_REQUEST, ErrorCode.E400, "인증에 실패했습니다.", LogLevel.WARN),
    INVALID_ACCESS_PATH(HttpStatus.BAD_REQUEST, ErrorCode.E400, "잘못된 접근 경로입니다.", LogLevel.WARN),
    NOT_FOUND_DATA(HttpStatus.BAD_REQUEST, ErrorCode.E400, "해당 데이터를 찾을 수 없습니다.", LogLevel.WARN),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.E429, "너무 많은 요청을 보냈습니다.", LogLevel.WARN),
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    // Auth
    INVALID_OAUTH_USER(HttpStatus.BAD_REQUEST, ErrorCode.E3000, "존재하지 않는 OAuth 유저입니다.", LogLevel.WARN),
    MALFORMED_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E3001, "JWT가 손상되었습니다.", LogLevel.WARN),
    UNSUPPORTED_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E3002, "지원하지 않는 JWT 형식입니다.", LogLevel.WARN),
    EXPIRED_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E3003, "JWT 기한이 만료되었습니다.", LogLevel.WARN),
    INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, ErrorCode.E3004, "JWT Signature 검증에 실패했습니다.", LogLevel.WARN),
    INVALID_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E3005, "JWT가 유효하지 않습니다.", LogLevel.WARN),
    NOT_FOUND_SUBJECT(HttpStatus.BAD_REQUEST, ErrorCode.E3006, "Subject를 찾을 수 없습니다.", LogLevel.WARN),
    OAUTH_ACCESS_TOKEN_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E3007, "OAuth 엑세스 토큰은 null일 수 없습니다.", LogLevel.WARN),
    FCM_TOKEN_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E3008, "FCM 토큰은 null일 수 없습니다.", LogLevel.WARN),
    ACCOUNT_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E3009, "계정은 null일 수 없습니다.", LogLevel.WARN),
    INVALID_ACCOUNT_LENGTH(HttpStatus.BAD_REQUEST, ErrorCode.E3010, "계정 길이는 6~12자리여야 합니다.", LogLevel.WARN),
    PASSWORD_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E3011, "비밀번호는 null일 수 없습니다.", LogLevel.WARN),
    INVALID_PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, ErrorCode.E3012, "비밀번호 길이는 8~20자리여야 합니다.", LogLevel.WARN),
    NOT_VERIFIED_EMAIL(HttpStatus.BAD_REQUEST, ErrorCode.E3013, "이메일이 인증되지 않았습니다.", LogLevel.WARN),
    INVALID_ACCOUNT_FORMAT(HttpStatus.BAD_REQUEST, ErrorCode.E3014, "계정은 영문 또는 숫자를 포함해야 합니다.", LogLevel.WARN),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, ErrorCode.E3015, "비밀번호는 영문과 숫자를 포함해야 합니다.", LogLevel.WARN),
    NOT_FOUND_ACCOUNT(HttpStatus.BAD_REQUEST, ErrorCode.E3016, "존재하지 않는 계정 정보입니다.", LogLevel.WARN),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, ErrorCode.E3017, "비밀번호가 일치하지 않습니다.", LogLevel.WARN),
    INVALID_MEMBER_KEY(HttpStatus.BAD_REQUEST, ErrorCode.E3018, "멤버 key가 유효하지 않습니다.", LogLevel.WARN),

    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode errorCode, String message, LogLevel logLevel) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.logLevel = logLevel;
    }
}
