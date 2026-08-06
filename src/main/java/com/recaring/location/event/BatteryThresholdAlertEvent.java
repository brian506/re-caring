package com.recaring.location.event;

import java.time.LocalDateTime;

public record BatteryThresholdAlertEvent(String memberKey, int thresholdPercent, LocalDateTime detectedAt) {
}
