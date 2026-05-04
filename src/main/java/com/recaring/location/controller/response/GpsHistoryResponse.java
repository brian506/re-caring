package com.recaring.location.controller.response;

import com.recaring.location.vo.Gps;

import java.time.LocalDateTime;

public record GpsHistoryResponse(
        double lat,
        double lng,
        LocalDateTime recordedAt
) {
    public static GpsHistoryResponse from(Gps gps) {
        return new GpsHistoryResponse(gps.lat(), gps.lng(), gps.recordedAt());
    }
}
