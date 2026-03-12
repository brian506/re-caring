package com.recaring.sms.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.regex.Pattern;

public record SmsCode(String value) {

    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");

    public SmsCode {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorType.INVALID_VERIFICATION_CODE);
        }
        if (!CODE_PATTERN.matcher(value).matches()) {
            throw new AppException(ErrorType.INVALID_VERIFICATION_CODE);
        }
    }

    public boolean matches(SmsCode other) {
        return this.value.equals(other.value);
    }
}
