package com.recaring.location.vo;

import java.time.LocalDateTime;

public record AnomalyEventPeriod(
        String eventId,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime lastAlertAt
) {
    public boolean isOngoing() {
        return endedAt == null;
    }

    public LocalDateTime routeEndAt() {
        return endedAt != null ? endedAt : lastAlertAt;
    }
}
