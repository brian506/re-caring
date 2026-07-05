package com.recaring.safezone.vo;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;

public record SafeZoneUpdate(
        String name,
        String address,
        double latitude,
        double longitude,
        SafeZoneRadius radius
) {
}
