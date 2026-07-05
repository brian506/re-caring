package com.recaring.safezone.vo;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;

public record SafeZoneCreation(
        String wardMemberKey,
        String name,
        String address,
        double latitude,
        double longitude,
        SafeZoneRadius radius
) {
}
