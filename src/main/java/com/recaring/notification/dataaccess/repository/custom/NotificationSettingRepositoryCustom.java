package com.recaring.notification.dataaccess.repository.custom;

public interface NotificationSettingRepositoryCustom {

    void deleteByWardMemberKey(String wardMemberKey);

    void updateSafeZone(String wardMemberKey, boolean entryEnabled, boolean exitEnabled);

    void updateAnomaly(
            String wardMemberKey,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    );

    void updateEmergencyCall(String wardMemberKey, boolean enabled);

    void updateBattery(String wardMemberKey, boolean lowBatteryEnabled, String batteryThresholdPercents);
}
