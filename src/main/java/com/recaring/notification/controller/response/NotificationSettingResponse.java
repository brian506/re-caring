package com.recaring.notification.controller.response;

import com.recaring.notification.vo.NotificationSettings;

public record NotificationSettingResponse(
        SafeZoneSettingResponse safeZone,
        DangerZoneSettingResponse dangerZone,
        AnomalySettingResponse anomaly,
        EmergencyCallSettingResponse emergencyCall,
        BatterySettingResponse battery
) {
    public static NotificationSettingResponse from(NotificationSettings settings) {
        return new NotificationSettingResponse(
                SafeZoneSettingResponse.from(settings.safeZone()),
                DangerZoneSettingResponse.from(settings.dangerZone()),
                AnomalySettingResponse.from(settings.anomaly()),
                EmergencyCallSettingResponse.from(settings.emergencyCall()),
                BatterySettingResponse.from(settings.battery())
        );
    }
}
