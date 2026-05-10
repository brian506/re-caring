package com.recaring.notification.business.command;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record UpdateEmergencyCallNotificationSettingCommand(
        String wardKey,
        boolean enabled
) {
    public UpdateEmergencyCallNotificationSettingCommand {
        if (wardKey == null || wardKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
    }
}
