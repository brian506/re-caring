package com.recaring.safezone.vo;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record SafeZoneCreation(
        String wardMemberKey,
        String name,
        String address,
        double latitude,
        double longitude,
        SafeZoneRadius radius
) {
    public SafeZoneCreation {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new AppException(ErrorType.INVALID_SAFE_ZONE_COORDINATE);
        }
    }
}
