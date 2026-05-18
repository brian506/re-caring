package com.recaring.safezone.controller.request;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;

public record CreateSafeZoneCommand(
        String wardMemberKey,
        String name,
        String address,
        double latitude,
        double longitude,
        SafeZoneRadius radius
) {
}
