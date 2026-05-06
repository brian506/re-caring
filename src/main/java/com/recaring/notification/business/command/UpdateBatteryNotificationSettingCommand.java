package com.recaring.notification.business.command;

import com.recaring.notification.vo.BatteryThreshold;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record UpdateBatteryNotificationSettingCommand(
        String wardKey,
        boolean lowBatteryEnabled,
        BatteryThreshold threshold
) {
    public UpdateBatteryNotificationSettingCommand {
        if (wardKey == null || wardKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
        if (threshold == null) {
            throw new AppException(ErrorType.INVALID_BATTERY_THRESHOLD);
        }
    }
}
