package com.recaring.notification.dataaccess.repository.custom;

public interface NotificationSettingRepositoryCustom {

    void deleteByMemberKey(String memberKey);

    void updateSafeZone(String wardMemberKey, String caregiverMemberKey, boolean entryEnabled, boolean exitEnabled);

    void updateAnomaly(
            String wardMemberKey,
            String caregiverMemberKey,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    );

    void updateEmergencyCall(String wardMemberKey, String caregiverMemberKey, boolean enabled);

    void updateBattery(
            String wardMemberKey,
            String caregiverMemberKey,
            boolean lowBatteryEnabled,
            String batteryThresholdPercents
    );
}
