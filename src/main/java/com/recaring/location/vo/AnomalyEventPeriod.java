package com.recaring.location.vo;

import java.time.Duration;
import java.time.LocalDateTime;

public record AnomalyEventPeriod(
        String eventId,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    // EVENT_END가 꺼져 있으면 끝난 사건도 종료 시각이 오지 않는다. 경로가 무한정 늘어나지 않도록 막는다.
    private static final Duration MAX_ONGOING_SPAN = Duration.ofHours(24);

    public boolean isOngoing() {
        return endedAt == null;
    }

    public LocalDateTime routeEndAt(LocalDateTime now) {
        if (endedAt != null) {
            return endedAt;
        }
        LocalDateTime cap = startedAt.plus(MAX_ONGOING_SPAN);
        return now.isBefore(cap) ? now : cap;
    }
}
