package com.recaring.notification.controller.response;

import com.recaring.notification.business.NotificationSettingInfo;

import java.util.List;

public record NotificationSettingResponse(
        SafeZoneSettingResponse safeZone,
        AnomalySettingResponse anomaly,
        EmergencyCallSettingResponse emergencyCall,
        BatterySettingResponse battery
) {
    public static NotificationSettingResponse from(NotificationSettingInfo info) {
        return new NotificationSettingResponse(
                SafeZoneSettingResponse.from(info.safeZone()),
                AnomalySettingResponse.from(info.anomaly()),
                EmergencyCallSettingResponse.from(info.emergencyCall()),
                BatterySettingResponse.from(info.battery())
        );
    }

    public record SafeZoneSettingResponse(
            boolean entryEnabled,
            boolean exitEnabled
    ) {
        private static SafeZoneSettingResponse from(NotificationSettingInfo.SafeZoneSettingInfo info) {
            return new SafeZoneSettingResponse(info.entryEnabled(), info.exitEnabled());
        }
    }

    public record AnomalySettingResponse(
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            String sensitivity,
            List<String> sensitivityOptions
    ) {
        private static AnomalySettingResponse from(NotificationSettingInfo.AnomalySettingInfo info) {
            return new AnomalySettingResponse(
                    info.routeDeviationEnabled(),
                    info.speedAnomalyEnabled(),
                    info.wanderingAnomalyEnabled(),
                    info.sensitivity(),
                    info.sensitivityOptions()
            );
        }
    }

    public record EmergencyCallSettingResponse(
            boolean enabled
    ) {
        private static EmergencyCallSettingResponse from(NotificationSettingInfo.EmergencyCallSettingInfo info) {
            return new EmergencyCallSettingResponse(info.enabled());
        }
    }

    public record BatterySettingResponse(
            boolean lowBatteryEnabled,
            int thresholdPercent,
            List<Integer> thresholdOptions
    ) {
        private static BatterySettingResponse from(NotificationSettingInfo.BatterySettingInfo info) {
            return new BatterySettingResponse(
                    info.lowBatteryEnabled(),
                    info.thresholdPercent(),
                    info.thresholdOptions()
            );
        }
    }
}
