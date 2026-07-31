package com.recaring.location.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GpsDetectionMessage(
        String memberKey,
        DetectionPoint currentLocation,
        int collectionIntervalSeconds,
        DeviceState currentState,
        int batteryLowThresholdPercent,
        int batteryFullThresholdPercent,
        LocalDateTime evaluationTime
) {
    public static GpsDetectionMessage of(
            String memberKey,
            DetectionPoint currentLocation,
            int collectionIntervalSeconds,
            DeviceState currentState,
            int batteryLowThresholdPercent,
            int batteryFullThresholdPercent,
            LocalDateTime evaluationTime
    ) {
        return new GpsDetectionMessage(
                memberKey, currentLocation, collectionIntervalSeconds, currentState,
                batteryLowThresholdPercent, batteryFullThresholdPercent, evaluationTime);
    }
}
