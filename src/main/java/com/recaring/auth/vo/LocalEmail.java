package com.recaring.auth.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.regex.Pattern;

public record LocalEmail(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public LocalEmail {
        validateEmail(value);
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorType.EMAIL_IS_NULL);
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new AppException(ErrorType.INVALID_EMAIL_FORMAT);
        }
    }
}
