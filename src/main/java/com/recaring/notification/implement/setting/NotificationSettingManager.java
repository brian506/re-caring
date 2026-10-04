package com.recaring.notification.implement.setting;

import com.recaring.location.implement.detection.BatteryAlertStateManager;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.vo.BatteryThresholds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationSettingManager {

    private final NotificationSettingRepository notificationSettingRepository;
    private final BatteryAlertStateManager batteryAlertStateManager;

    @Transactional
    public void deleteByRelationship(String wardKey, String caregiverKey) {
        notificationSettingRepository.deleteByRelationship(wardKey, caregiverKey);
        batteryAlertStateManager.delete(wardKey, caregiverKey);
    }

    @Transactional
    public void updateSafeZone(String wardKey, String caregiverKey, boolean entryEnabled, boolean exitEnabled) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey, caregiverKey);
        notificationSettingRepository.updateSafeZone(wardKey, caregiverKey, entryEnabled, exitEnabled);
    }

    @Transactional
    public void updateAnomaly(
            String wardKey,
            String caregiverKey,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    ) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey, caregiverKey);
        notificationSettingRepository.updateAnomaly(
                wardKey, caregiverKey,
                speedAnomalyEnabled, wanderingAnomalyEnabled, abnormalDwellingEnabled,
                routeDeviationEnabled, timeAnomalyEnabled);
    }

    @Transactional
    public void updateEmergencyCall(String wardKey, String caregiverKey, boolean enabled) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey, caregiverKey);
        notificationSettingRepository.updateEmergencyCall(wardKey, caregiverKey, enabled);
    }

    @Transactional
    public void updateBattery(String wardKey, String caregiverKey, boolean lowBatteryEnabled, BatteryThresholds thresholds) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey, caregiverKey);
        notificationSettingRepository.updateBattery(wardKey, caregiverKey, lowBatteryEnabled, thresholds.format());
        if (!lowBatteryEnabled) {
            batteryAlertStateManager.delete(wardKey, caregiverKey);
        }
    }
}
