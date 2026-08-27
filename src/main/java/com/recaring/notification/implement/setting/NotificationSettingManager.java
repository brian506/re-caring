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
    public void addDefaultIfAbsent(String wardKey) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
    }

    @Transactional
    public void updateSafeZone(String wardKey, boolean entryEnabled, boolean exitEnabled) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateSafeZone(wardKey, entryEnabled, exitEnabled);
    }

    @Transactional
    public void updateAnomaly(
            String wardKey,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    ) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateAnomaly(
                wardKey,
                speedAnomalyEnabled, wanderingAnomalyEnabled, abnormalDwellingEnabled,
                routeDeviationEnabled, timeAnomalyEnabled);
    }

    @Transactional
    public void updateEmergencyCall(String wardKey, boolean enabled) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateEmergencyCall(wardKey, enabled);
    }

    @Transactional
    public void updateBattery(String wardKey, boolean lowBatteryEnabled, BatteryThresholds thresholds) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateBattery(wardKey, lowBatteryEnabled, thresholds.format());
        if (!lowBatteryEnabled) {
            batteryAlertStateManager.delete(wardKey);
        }
    }
}
