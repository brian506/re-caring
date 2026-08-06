package com.recaring.location.controller.request;

import com.recaring.location.vo.Gps;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record GpsRequest(
        @NotNull
        @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull
        @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,

        @PositiveOrZero
        Double accuracy,

        @Min(0) @Max(100)
        Integer battery,

        @PositiveOrZero
        @Schema(description = "이동 속도 (m/s). 기기가 제공하지 않으면 생략", example = "1.4")
        Double speed,

        @Schema(description = "기기가 좌표를 측정한 시각 (ISO-8601, 오프셋 포함). 생략 시 시간 기반 분석에서 제외된다",
                example = "2026-08-04T14:23:11Z")
        Instant measuredAt
) {
    public Gps toGps(LocalDateTime receivedAt) {
        return new Gps(latitude, longitude, receivedAt,
                accuracy, battery, speed, toUtcLocalDateTime(measuredAt));
    }

    private static LocalDateTime toUtcLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
