package com.recaring.notification.controller.request;

import com.recaring.notification.business.command.UpdateEmergencyCallNotificationSettingCommand;
import jakarta.validation.constraints.NotNull;

public record UpdateEmergencyCallNotificationSettingRequest(
        @NotNull Boolean enabled
) {
    public UpdateEmergencyCallNotificationSettingCommand toCommand(String wardKey) {
        return new UpdateEmergencyCallNotificationSettingCommand(wardKey, enabled);
    }
}
