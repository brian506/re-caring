package com.recaring.notification.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record FcmToken(String value) {

    private static final int MAX_LENGTH = 512;

    public FcmToken {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorType.INVALID_FCM_DEVICE_TOKEN);
        }
        if (value.length() > MAX_LENGTH) {
            throw new AppException(ErrorType.INVALID_FCM_DEVICE_TOKEN);
        }
    }
}
