package com.recaring.notification.business.command;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record UpdateAnomalyNotificationSettingCommand(
        String wardKey,
        boolean speedAnomalyEnabled,
        boolean wanderingAnomalyEnabled,
        boolean abnormalDwellingEnabled,
        boolean routeDeviationEnabled,
        boolean timeAnomalyEnabled
) {
    public UpdateAnomalyNotificationSettingCommand {
        if (wardKey == null || wardKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
    }
}
