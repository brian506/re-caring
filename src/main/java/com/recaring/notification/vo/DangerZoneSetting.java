package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.NotificationSetting;

public record DangerZoneSetting(
        boolean entryEnabled,
        boolean exitEnabled
) {
    public static DangerZoneSetting from(NotificationSetting setting) {
        return new DangerZoneSetting(
                setting.isDangerZoneEntryEnabled(),
                setting.isDangerZoneExitEnabled()
        );
    }
}
