package com.recaring.location.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.recaring.location.dataaccess.entity.GpsHistory;

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
}
