package com.recaring.notification.controller.request;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import jakarta.validation.constraints.NotNull;

public record UpdateAnomalyNotificationSettingRequest(
        @NotNull Boolean speedAnomalyEnabled,
        @NotNull Boolean wanderingAnomalyEnabled,
        @NotNull Boolean abnormalDwellingEnabled,
        @NotNull Boolean routeDeviationEnabled,
        @NotNull Boolean timeAnomalyEnabled
) {
    public UpdateAnomalyNotificationSettingCommand toCommand(String wardKey) {
        return new UpdateAnomalyNotificationSettingCommand(
                wardKey,
                speedAnomalyEnabled,
                wanderingAnomalyEnabled,
                abnormalDwellingEnabled,
                routeDeviationEnabled,
                timeAnomalyEnabled
        );
    }
}
