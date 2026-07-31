package com.recaring.location.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DetectionPoint(
        double latitude,
        double longitude,
        Double accuracy,
        Integer battery,
        LocalDateTime recordedAt
) {
    public static DetectionPoint from(Gps gps) {
        return new DetectionPoint(
                gps.latitude(), gps.longitude(), gps.accuracy(), gps.battery(),
                gps.recordedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
