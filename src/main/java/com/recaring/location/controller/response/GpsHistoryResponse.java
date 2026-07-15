package com.recaring.location.controller.response;

import com.recaring.location.vo.Gps;

import java.time.Instant;
import java.time.ZoneOffset;

public record GpsHistoryResponse(
        double latitude,
        double longitude,
        Instant recordedAt
) {
    public static GpsHistoryResponse from(Gps gps) {
        return new GpsHistoryResponse(
                gps.latitude(),
                gps.longitude(),
                gps.recordedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
