package com.recaring.notification.controller.request;

import com.recaring.notification.business.command.UpdateSafeZoneNotificationSettingCommand;
import jakarta.validation.constraints.NotNull;

public record UpdateSafeZoneNotificationSettingRequest(
        @NotNull Boolean entryEnabled,
        @NotNull Boolean exitEnabled
) {
    public UpdateSafeZoneNotificationSettingCommand toCommand(String wardKey) {
        return new UpdateSafeZoneNotificationSettingCommand(
                wardKey,
                entryEnabled,
                exitEnabled
        );
    }
}
