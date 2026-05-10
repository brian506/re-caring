package com.recaring.notification.controller.request;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.vo.AnomalySensitivity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAnomalyNotificationSettingRequest(
        @NotNull Boolean routeDeviationEnabled,
        @NotNull Boolean speedAnomalyEnabled,
        @NotNull Boolean wanderingAnomalyEnabled,
        @NotBlank String sensitivity
) {
    public UpdateAnomalyNotificationSettingCommand toCommand(String wardKey) {
        return new UpdateAnomalyNotificationSettingCommand(
                wardKey,
                routeDeviationEnabled,
                speedAnomalyEnabled,
                wanderingAnomalyEnabled,
                AnomalySensitivity.from(sensitivity)
        );
    }
}
