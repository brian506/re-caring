package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.vo.AnomalySensitivity;

public interface NotificationSettingRepositoryCustom {

    void deleteByWardMemberKey(String wardMemberKey);

    void updateSafeZone(String wardMemberKey, boolean entryEnabled, boolean exitEnabled);

    void updateAnomaly(
            String wardMemberKey,
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            AnomalySensitivity routeDeviationSensitivity,
            AnomalySensitivity speedAnomalySensitivity,
            AnomalySensitivity wanderingAnomalySensitivity
    );

    void updateEmergencyCall(String wardMemberKey, boolean enabled);

    void updateBattery(String wardMemberKey, boolean lowBatteryEnabled, String batteryThresholdPercents);
}
