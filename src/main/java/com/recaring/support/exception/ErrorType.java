package com.recaring.support.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {

    // Global
    REQUIRED_AUTH(HttpStatus.UNAUTHORIZED, ErrorCode.E401, "리소스에 접근하기 위한 인증이 필요합니다.", LogLevel.WARN),
    FAILED_AUTH(HttpStatus.BAD_REQUEST, ErrorCode.E400, "인증에 실패했습니다.", LogLevel.WARN),
    INVALID_ACCESS_PATH(HttpStatus.BAD_REQUEST, ErrorCode.E400, "잘못된 접근 경로입니다.", LogLevel.WARN),
    NOT_FOUND_DATA(HttpStatus.BAD_REQUEST, ErrorCode.E400, "해당 데이터를 찾을 수 없습니다.", LogLevel.WARN),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.E429, "너무 많은 요청을 보냈습니다.", LogLevel.WARN),
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    // Auth (E2xxx)
    INVALID_OAUTH_USER(HttpStatus.BAD_REQUEST, ErrorCode.E2000, "존재하지 않는 OAuth 유저입니다.", LogLevel.WARN),
    MALFORMED_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E2001, "JWT가 손상되었습니다.", LogLevel.WARN),
    UNSUPPORTED_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E2002, "지원하지 않는 JWT 형식입니다.", LogLevel.WARN),
    EXPIRED_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E2003, "JWT 기한이 만료되었습니다.", LogLevel.WARN),
    INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, ErrorCode.E2004, "JWT Signature 검증에 실패했습니다.", LogLevel.WARN),
    INVALID_JWT(HttpStatus.BAD_REQUEST, ErrorCode.E2005, "JWT가 유효하지 않습니다.", LogLevel.WARN),
    NOT_FOUND_SUBJECT(HttpStatus.BAD_REQUEST, ErrorCode.E2006, "Subject를 찾을 수 없습니다.", LogLevel.WARN),
    OAUTH_ACCESS_TOKEN_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E2007, "OAuth 엑세스 토큰은 null일 수 없습니다.", LogLevel.WARN),
    FCM_TOKEN_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E2008, "FCM 토큰은 null일 수 없습니다.", LogLevel.WARN),
    ACCOUNT_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E2009, "계정은 null일 수 없습니다.", LogLevel.WARN),
    INVALID_ACCOUNT_LENGTH(HttpStatus.BAD_REQUEST, ErrorCode.E2010, "계정 길이는 6~12자리여야 합니다.", LogLevel.WARN),
    PASSWORD_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E2011, "비밀번호는 null일 수 없습니다.", LogLevel.WARN),
    INVALID_PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, ErrorCode.E2012, "비밀번호 길이는 8~20자리여야 합니다.", LogLevel.WARN),
    NOT_VERIFIED_EMAIL(HttpStatus.BAD_REQUEST, ErrorCode.E2013, "이메일이 인증되지 않았습니다.", LogLevel.WARN),
    INVALID_ACCOUNT_FORMAT(HttpStatus.BAD_REQUEST, ErrorCode.E2014, "계정은 영문 또는 숫자를 포함해야 합니다.", LogLevel.WARN),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, ErrorCode.E2015, "비밀번호는 영문과 숫자를 포함해야 합니다.", LogLevel.WARN),
    NOT_FOUND_ACCOUNT(HttpStatus.BAD_REQUEST, ErrorCode.E2016, "존재하지 않는 계정 정보입니다.", LogLevel.WARN),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, ErrorCode.E2017, "비밀번호가 일치하지 않습니다.", LogLevel.WARN),
    INVALID_MEMBER_KEY(HttpStatus.BAD_REQUEST, ErrorCode.E2018, "멤버 key가 유효하지 않습니다.", LogLevel.WARN),

    // Member (E3xxx)
    EMAIL_IS_NULL(HttpStatus.BAD_REQUEST, ErrorCode.E3000, "이메일은 필수 입력값입니다.", LogLevel.WARN),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, ErrorCode.E3001, "올바른 이메일 형식이 아닙니다.", LogLevel.WARN),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, ErrorCode.E3002, "이메일이 유효하지 않습니다.", LogLevel.WARN),
    PREMIUM_ONLY(HttpStatus.BAD_REQUEST, ErrorCode.E3003, "프리미엄 구독을 한 회원만 접근 가능합니다", LogLevel.WARN),
    BASIC_ONLY(HttpStatus.BAD_REQUEST, ErrorCode.E3004, "베이식 구독을 한 회원만 접근 가능합니다", LogLevel.WARN),
    SUBSCRIPTION_ONLY(HttpStatus.BAD_REQUEST, ErrorCode.E3005, "멤버십 결제가 필요한 기능입니다.", LogLevel.WARN),

    // SMS / Phone Verification (E4xxx)
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, ErrorCode.E4000, "인증번호가 만료되었습니다.", LogLevel.WARN),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, ErrorCode.E4001, "인증번호가 일치하지 않습니다.", LogLevel.WARN),
    NOT_VERIFIED_PHONE(HttpStatus.BAD_REQUEST, ErrorCode.E4002, "휴대폰 인증이 완료되지 않았습니다.", LogLevel.WARN),
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E4003, "SMS 발송에 실패했습니다.", LogLevel.ERROR),
    INVALID_PHONE_FORMAT(HttpStatus.BAD_REQUEST, ErrorCode.E4004, "올바른 휴대폰 번호 형식이 아닙니다.", LogLevel.WARN),

    // Care (E5xxx)
    CARE_CAREGIVER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, ErrorCode.E5000, "보호 대상자의 보호자/관리자는 최대 3명입니다.", LogLevel.WARN),
    ALREADY_CARE_RELATIONSHIP(HttpStatus.BAD_REQUEST, ErrorCode.E5001, "이미 등록된 보호자/관리자입니다.", LogLevel.WARN),
    NOT_GUARDIAN_OF_WARD(HttpStatus.FORBIDDEN, ErrorCode.E5002, "해당 보호 대상자의 보호자가 아닙니다.", LogLevel.WARN),
    NOT_FOUND_CARE_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E5003, "존재하지 않는 케어 요청입니다.", LogLevel.WARN),
    NOT_CARE_REQUEST_TARGET(HttpStatus.FORBIDDEN, ErrorCode.E5004, "해당 케어 요청의 수신자가 아닙니다.", LogLevel.WARN),
    EXPIRED_CARE_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E5005, "만료된 케어 요청입니다.", LogLevel.WARN),
    ALREADY_PROCESSED_CARE_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E5006, "이미 처리된 케어 요청입니다.", LogLevel.WARN),
    INVALID_WARD_ROLE(HttpStatus.BAD_REQUEST, ErrorCode.E5007, "보호 대상자로 가입한 회원만 대상자로 추가할 수 있습니다.", LogLevel.WARN),
    INVALID_CAREGIVER_ROLE(HttpStatus.BAD_REQUEST, ErrorCode.E5008, "보호자로 가입한 회원만 보호자/관리자로 추가할 수 있습니다.", LogLevel.WARN),
    ALREADY_PENDING_CARE_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E5009, "이미 대기 중인 케어 요청이 존재합니다.", LogLevel.WARN),
    WARD_KEY_REQUIRED(HttpStatus.BAD_REQUEST, ErrorCode.E5010, "보호자를 추가하려면 보호 대상자 키가 필요합니다.", LogLevel.WARN),
    NOT_FOUND_CARE_RELATIONSHIP(HttpStatus.BAD_REQUEST, ErrorCode.E5011, "존재하지 않는 케어 관계입니다.", LogLevel.WARN),
    NOT_GUARDIAN_ROLE_IN_CARE(HttpStatus.FORBIDDEN, ErrorCode.E5012, "해당 보호 대상자의 보호자(GUARDIAN) 역할이 아닙니다.", LogLevel.WARN),

    // Location (E6xxx)
    NOT_WARD_MEMBER(HttpStatus.FORBIDDEN, ErrorCode.E6000, "보호 대상자로 가입한 회원만 GPS를 전송할 수 있습니다.", LogLevel.WARN),
    NOT_CARE_RELATED_WARD(HttpStatus.FORBIDDEN, ErrorCode.E6001, "케어 관계가 없는 보호 대상자입니다.", LogLevel.WARN),
    INVALID_LOCATION_COLLECTION_INTERVAL(HttpStatus.BAD_REQUEST, ErrorCode.E6002, "지원하지 않는 위치 수집 주기입니다.", LogLevel.WARN),

    // Device (E7xxx)
    INVALID_DEVICE_TOKEN(HttpStatus.UNAUTHORIZED, ErrorCode.E7000, "유효하지 않은 Device Token입니다.", LogLevel.WARN),

    // SafeZone (E8xxx)
    NOT_FOUND_SAFE_ZONE(HttpStatus.BAD_REQUEST, ErrorCode.E8000, "존재하지 않는 안심존입니다.", LogLevel.WARN),
    NOT_CAREGIVER_OF_WARD(HttpStatus.FORBIDDEN, ErrorCode.E8001, "해당 보호 대상자의 보호자/관계자가 아닙니다.", LogLevel.WARN);

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
