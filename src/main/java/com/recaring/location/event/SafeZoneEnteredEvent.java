package com.recaring.location.event;

import java.time.LocalDateTime;

public record SafeZoneEnteredEvent(
        String wardMemberKey,
        String safeZoneKey,
        String safeZoneName,
        double latitude,
        double longitude,
        LocalDateTime detectedAt
) {
}
