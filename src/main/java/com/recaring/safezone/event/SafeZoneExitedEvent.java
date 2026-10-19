package com.recaring.safezone.event;

import com.recaring.safezone.vo.SafeZoneType;

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
