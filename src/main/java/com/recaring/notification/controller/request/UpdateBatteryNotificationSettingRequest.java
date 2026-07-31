package com.recaring.notification.controller.request;

import com.recaring.notification.vo.BatteryThreshold;
import com.recaring.notification.vo.BatteryThresholdRange;
import jakarta.validation.constraints.NotNull;

public record UpdateBatteryNotificationSettingRequest(
        @NotNull Boolean lowBatteryEnabled,
        @NotNull Integer lowThresholdPercent,
        @NotNull Integer fullThresholdPercent
) {
    public BatteryThresholdRange toThresholdRange() {
        return new BatteryThresholdRange(
                new BatteryThreshold(lowThresholdPercent),
                new BatteryThreshold(fullThresholdPercent)
        );
    }
}
