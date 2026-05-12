package com.recaring.safezone.vo;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;

public record SafeZoneInfo(
        String safeZoneKey,
        String name,
        String address,
        double latitude,
        double longitude,
        SafeZoneRadius radius
) {
    public static SafeZoneInfo from(SafeZone entity) {
        return new SafeZoneInfo(
                entity.getSafeZoneKey(),
                entity.getName(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getRadius()
        );
    }
}
