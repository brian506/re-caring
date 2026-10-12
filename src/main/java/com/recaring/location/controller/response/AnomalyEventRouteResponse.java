package com.recaring.location.controller.response;

import com.recaring.location.vo.AnomalyEventPeriod;
import com.recaring.location.vo.AnomalyEventRoute;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public record AnomalyEventRouteResponse(
        String eventId,
        Instant startedAt,
        Instant endedAt,
        boolean ongoing,
        List<GpsHistoryResponse> route
) {
    private static final ZoneId EVENT_ZONE = ZoneId.of("Asia/Seoul");

    public static AnomalyEventRouteResponse from(AnomalyEventRoute eventRoute) {
        AnomalyEventPeriod period = eventRoute.period();
        return new AnomalyEventRouteResponse(
                period.eventId(),
                toInstant(period.startedAt()),
                toInstant(period.endedAt()),
                period.isOngoing(),
                eventRoute.route().stream()
                        .map(GpsHistoryResponse::from)
                        .toList()
        );
    }

    private static Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(EVENT_ZONE).toInstant();
    }
}
