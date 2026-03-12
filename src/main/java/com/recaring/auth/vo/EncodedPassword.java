package com.recaring.auth.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record EncodedPassword(String password) {

    public EncodedPassword {
        if (password == null || password.isBlank()) {
            throw new AppException(ErrorType.PASSWORD_IS_NULL);
        }
    }
}
