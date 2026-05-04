package com.recaring.location.vo;

import com.recaring.location.dataaccess.entity.GpsHistory;

import java.time.LocalDateTime;

public record Gps(
        double lat,
        double lng,
        LocalDateTime recordedAt
) {
    public static Gps from(GpsHistory entity) {
        return new Gps(entity.getLatitude(), entity.getLongitude(), entity.getRecordedAt());
    }
}
