package com.recaring.location.controller.response;

import com.recaring.location.vo.Gps;

import java.time.Instant;
import java.time.ZoneId;

public record GpsHistoryResponse(
        double latitude,
        double longitude,
        Instant recordedAt
) {
    private static final ZoneId RECORDED_AT_ZONE = ZoneId.of("Asia/Seoul");

    public static GpsHistoryResponse from(Gps gps) {
        return new GpsHistoryResponse(
                gps.latitude(),
                gps.longitude(),
                gps.recordedAt().atZone(RECORDED_AT_ZONE).toInstant()
        );
    }
}
