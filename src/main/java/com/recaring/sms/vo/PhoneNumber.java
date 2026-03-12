package com.recaring.sms.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.regex.Pattern;

public record PhoneNumber(String value) {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorType.INVALID_PHONE_FORMAT);
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new AppException(ErrorType.INVALID_PHONE_FORMAT);
        }
    }
}
