package com.recaring.safezone.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record SafeZoneRadius(int meters) {
    private static final int MIN_METERS = 100;
    private static final int MAX_METERS = 2000;

    public SafeZoneRadius {
        if (meters < MIN_METERS || meters > MAX_METERS) {
            throw new AppException(ErrorType.INVALID_SAFE_ZONE_RADIUS);
        }
    }
}
