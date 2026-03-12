package com.recaring.auth.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record EncodedPassword(String value) {

    public EncodedPassword {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorType.PASSWORD_IS_NULL);
        }
    }
}
