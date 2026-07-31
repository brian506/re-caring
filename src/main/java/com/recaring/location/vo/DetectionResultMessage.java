package com.recaring.location.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DetectionResultMessage(
        String memberKey,
        DeviceState previousState,
        DeviceState newState,
        String breachedSafeZoneKey,
        LocalDateTime detectedAt,
        String detector
) {
}
