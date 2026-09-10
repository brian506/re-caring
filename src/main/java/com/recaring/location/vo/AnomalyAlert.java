package com.recaring.location.vo;

import java.time.LocalDateTime;

public record AnomalyAlert(
        String wardMemberKey,
        DetectionType detectionType,
        double score,
        LocalDateTime recordedAt,
        double latitude,
        double longitude,
        String evidence
) {
}
