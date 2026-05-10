package com.recaring.notification.business.command;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record UpdateSafeZoneNotificationSettingCommand(
        String wardKey,
        boolean entryEnabled,
        boolean exitEnabled
) {
    public UpdateSafeZoneNotificationSettingCommand {
        if (wardKey == null || wardKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
    }
}
