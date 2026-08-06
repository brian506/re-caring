package com.recaring.location.vo;

import java.time.LocalDateTime;

public record GpsReport(
        double latitude,
        double longitude,
        Double accuracy,
        Integer battery,
        Double speed,
        LocalDateTime measuredAt
) {
    public Gps toGps(LocalDateTime receivedAt) {
        return new Gps(latitude, longitude, receivedAt, accuracy, battery, speed, measuredAt);
    }
}
