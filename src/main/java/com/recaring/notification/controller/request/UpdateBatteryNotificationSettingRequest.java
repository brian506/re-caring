package com.recaring.notification.controller.request;

import com.recaring.notification.vo.BatteryThreshold;
import jakarta.validation.constraints.NotNull;

public record UpdateBatteryNotificationSettingRequest(
        @NotNull Boolean lowBatteryEnabled,
        @NotNull Integer thresholdPercent
) {
    public BatteryThreshold toThreshold() {
        return new BatteryThreshold(thresholdPercent);
    }
}
