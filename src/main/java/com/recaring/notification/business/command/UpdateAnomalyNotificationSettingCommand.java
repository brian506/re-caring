package com.recaring.notification.business.command;

import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record UpdateAnomalyNotificationSettingCommand(
        String wardKey,
        boolean routeDeviationEnabled,
        boolean speedAnomalyEnabled,
        boolean wanderingAnomalyEnabled,
        AnomalySensitivity sensitivity
) {
    public UpdateAnomalyNotificationSettingCommand {
        if (wardKey == null || wardKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
        if (sensitivity == null) {
            throw new AppException(ErrorType.INVALID_NOTIFICATION_SENSITIVITY);
        }
    }
}
