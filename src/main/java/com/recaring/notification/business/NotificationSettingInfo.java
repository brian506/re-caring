package com.recaring.notification.business;

import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThreshold;
import com.recaring.notification.vo.BatteryThresholds;

import java.util.List;

public record NotificationSettingInfo(
        SafeZoneSettingInfo safeZone,
        AnomalySettingInfo anomaly,
        EmergencyCallSettingInfo emergencyCall,
        BatterySettingInfo battery
) {
    public static NotificationSettingInfo from(NotificationSetting setting) {
        return new NotificationSettingInfo(
                new SafeZoneSettingInfo(
                        setting.isSafeZoneEntryEnabled(),
                        setting.isSafeZoneExitEnabled()
                ),
                new AnomalySettingInfo(
                        setting.isRouteDeviationEnabled(),
                        setting.isSpeedAnomalyEnabled(),
                        setting.isWanderingAnomalyEnabled(),
                        setting.getRouteDeviationSensitivity().name(),
                        setting.getSpeedAnomalySensitivity().name(),
                        setting.getWanderingAnomalySensitivity().name(),
                        AnomalySensitivity.options()
                ),
                new EmergencyCallSettingInfo(setting.isEmergencyCallEnabled()),
                new BatterySettingInfo(
                        setting.isLowBatteryEnabled(),
                        BatteryThresholds.parse(setting.getBatteryThresholdPercents()).percents(),
                        BatteryThreshold.options()
                )
        );
    }

    public record SafeZoneSettingInfo(
            boolean entryEnabled,
            boolean exitEnabled
    ) {
    }

    public record AnomalySettingInfo(
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            String routeDeviationSensitivity,
            String speedAnomalySensitivity,
            String wanderingAnomalySensitivity,
            List<String> sensitivityOptions
    ) {
    }

    public record EmergencyCallSettingInfo(
            boolean enabled
    ) {
    }

    public record BatterySettingInfo(
            boolean lowBatteryEnabled,
            List<Integer> thresholdPercents,
            List<Integer> thresholdOptions
    ) {
    }
}
