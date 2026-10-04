package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.NotificationSetting;

public record NotificationSettings(
        SafeZoneSetting safeZone,
        DangerZoneSetting dangerZone,
        AnomalySetting anomaly,
        EmergencyCallSetting emergencyCall,
        BatterySetting battery
) {
    public static NotificationSettings from(NotificationSetting setting) {
        return new NotificationSettings(
                SafeZoneSetting.from(setting),
                DangerZoneSetting.from(setting),
                AnomalySetting.from(setting),
                EmergencyCallSetting.from(setting),
                BatterySetting.from(setting)
        );
    }
}
