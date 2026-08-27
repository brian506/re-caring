package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.NotificationSetting;

public record SafeZoneSetting(
        boolean entryEnabled,
        boolean exitEnabled
) {
    public static SafeZoneSetting from(NotificationSetting setting) {
        return new SafeZoneSetting(
                setting.isSafeZoneEntryEnabled(),
                setting.isSafeZoneExitEnabled()
        );
    }
}
