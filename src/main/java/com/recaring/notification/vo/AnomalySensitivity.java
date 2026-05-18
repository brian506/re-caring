package com.recaring.notification.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.Arrays;
import java.util.List;

public enum AnomalySensitivity {
    VERY_LOW,
    LOW,
    NORMAL,
    HIGH,
    VERY_HIGH;

    public static final AnomalySensitivity DEFAULT = NORMAL;

    public static AnomalySensitivity from(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorType.INVALID_NOTIFICATION_SENSITIVITY);
        }
        return Arrays.stream(values())
                .filter(sensitivity -> sensitivity.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorType.INVALID_NOTIFICATION_SENSITIVITY));
    }

    public static List<String> options() {
        return Arrays.stream(values())
                .map(AnomalySensitivity::name)
                .toList();
    }
}
