package com.recaring.location.event;

import com.recaring.safezone.dataaccess.entity.SafeZoneType;

import java.time.LocalDateTime;

public record SafeZoneExitedEvent(
        String wardMemberKey,
        String safeZoneKey,
        String safeZoneName,
        SafeZoneType zoneType,
        double latitude,
        double longitude,
        LocalDateTime detectedAt
) {
}
