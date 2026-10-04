package com.recaring.location.event;

import java.time.LocalDateTime;
import java.util.List;

public record BatteryThresholdAlertEvent(
        String memberKey,
        int thresholdPercent,
        LocalDateTime detectedAt,
        List<String> recipientKeys
) {
}
