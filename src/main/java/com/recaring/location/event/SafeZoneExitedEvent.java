package com.recaring.location.event;

import java.time.LocalDateTime;

public record SafeZoneExitedEvent(
        String wardMemberKey,
        String safeZoneKey,
        String safeZoneName,
        LocalDateTime detectedAt
) {
}
