package com.recaring.notification.controller.request;

import com.recaring.notification.business.command.UpdateBatteryNotificationSettingCommand;
import com.recaring.notification.vo.BatteryThreshold;
import jakarta.validation.constraints.NotNull;

public record UpdateBatteryNotificationSettingRequest(
        @NotNull Boolean lowBatteryEnabled,
        @NotNull Integer thresholdPercent
) {
    public UpdateBatteryNotificationSettingCommand toCommand(String wardKey) {
        return new UpdateBatteryNotificationSettingCommand(
                wardKey,
                lowBatteryEnabled,
                new BatteryThreshold(thresholdPercent)
        );
    }
}
