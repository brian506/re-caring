package com.recaring.location.vo;

import com.recaring.location.dataaccess.entity.GpsHistory;

import java.time.LocalDateTime;

public record Gps(
        double latitude,
        double longitude,
        LocalDateTime recordedAt,
        Double accuracy,
        Integer battery,
        Double speed,
        LocalDateTime measuredAt
) {
    public static Gps from(GpsHistory entity) {
        return new Gps(
                entity.getLatitude(), entity.getLongitude(), entity.getRecordedAt(),
                entity.getAccuracy(), entity.getBattery(),
                entity.getSpeed(), entity.getMeasuredAt());
    }
}
