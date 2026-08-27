package com.recaring.location.vo;

import java.time.LocalDateTime;

public record AnomalyAlert(
        String wardMemberKey,
        DetectionType detectionType,
        double score,
        LocalDateTime detectedAt,
        double latitude,
        double longitude,
        String evidence
) {
}
