package com.recaring.safezone.controller.response;

import com.recaring.safezone.vo.SafeZoneType;
import com.recaring.safezone.vo.SafeZoneInfo;

public record SafeZoneResponse(
        String safeZoneKey,
        String name,
        String address,
        double latitude,
        double longitude,
        int radiusMeters,
        SafeZoneType type
) {
    public static SafeZoneResponse from(SafeZoneInfo info) {
        return new SafeZoneResponse(
                info.safeZoneKey(),
                info.name(),
                info.address(),
                info.latitude(),
                info.longitude(),
                info.radiusMeters(),
                info.type()
        );
    }
}
