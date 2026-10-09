package com.recaring.location.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.recaring.location.dataaccess.entity.GpsHistory;

import java.time.Duration;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Gps(
        double latitude,
        double longitude,
        LocalDateTime recordedAt,
        Double accuracy,
        Integer battery,
        Double speed,
        LocalDateTime measuredAt
) {
    private static final double MAX_ACCURACY_METERS = 100.0;
    private static final double MAX_SPEED_METERS_PER_SECOND = 50.0;
    private static final Duration ANCHOR_EXPIRY = Duration.ofMinutes(10);
    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    public static Gps from(GpsHistory entity) {
        return new Gps(
                entity.getLatitude(), entity.getLongitude(), entity.getRecordedAt(),
                entity.getAccuracy(), entity.getBattery(),
                entity.getSpeed(), entity.getMeasuredAt());
    }

    // 기기가 좌표를 측정한 시각. 기기가 보고하지 않았을 때만 서버 수신 시각으로 대체한다.
    public LocalDateTime occurredAt() {
        return measuredAt != null ? measuredAt : recordedAt;
    }

    public boolean isAccurate() {
        return accuracy == null || accuracy <= MAX_ACCURACY_METERS;
    }

    // 기준점 만료는 서버 수신 시각으로 잰다. 기기 시계가 틀어져도 잘못된 기준점에 계속 묶이지 않게 하기 위함이다.
    public boolean isDisplayableAfter(Gps anchor) {
        if (!isAccurate()) {
            return false;
        }
        if (anchor == null || recordedAt.isAfter(anchor.recordedAt().plus(ANCHOR_EXPIRY))) {
            return true;
        }
        long elapsedMillis = Duration.between(anchor.occurredAt(), occurredAt()).toMillis();
        if (elapsedMillis < 0) {
            return false;
        }
        double elapsedSeconds = Math.max(elapsedMillis, 1_000) / 1_000.0;
        double travelledMeters = Math.max(0, distanceTo(anchor) - uncertaintyMeters() - anchor.uncertaintyMeters());
        return travelledMeters / elapsedSeconds <= MAX_SPEED_METERS_PER_SECOND;
    }

    private double uncertaintyMeters() {
        return accuracy == null ? 0 : accuracy;
    }

    private double distanceTo(Gps other) {
        double latRad = Math.toRadians(latitude);
        double otherLatRad = Math.toRadians(other.latitude());
        double deltaLat = Math.toRadians(other.latitude() - latitude);
        double deltaLon = Math.toRadians(other.longitude() - longitude);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad) * Math.cos(otherLatRad) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
