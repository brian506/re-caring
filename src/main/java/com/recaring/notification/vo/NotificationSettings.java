package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.NotificationSetting;

public record NotificationSettings(
        SafeZoneSetting safeZone,
        AnomalySetting anomaly,
        EmergencyCallSetting emergencyCall,
        BatterySetting battery
) {
    public static NotificationSettings from(NotificationSetting setting) {
        return new NotificationSettings(
                SafeZoneSetting.from(setting),
                AnomalySetting.from(setting),
                EmergencyCallSetting.from(setting),
                BatterySetting.from(setting)
        );
    }
}
