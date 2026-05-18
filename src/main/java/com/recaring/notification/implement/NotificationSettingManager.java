package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThreshold;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationSettingManager {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional
    public void addDefaultIfAbsent(String wardKey) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
    }

    @Transactional
    public void updateSafeZone(NotificationSetting setting, boolean entryEnabled, boolean exitEnabled) {
        setting.updateSafeZone(entryEnabled, exitEnabled);
        notificationSettingRepository.save(setting);
    }

    @Transactional
    public void updateAnomaly(
            NotificationSetting setting,
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            AnomalySensitivity sensitivity
    ) {
        setting.updateAnomaly(routeDeviationEnabled, speedAnomalyEnabled, wanderingAnomalyEnabled, sensitivity);
        notificationSettingRepository.save(setting);
    }

    @Transactional
    public void updateEmergencyCall(NotificationSetting setting, boolean enabled) {
        setting.updateEmergencyCall(enabled);
        notificationSettingRepository.save(setting);
    }

    @Transactional
    public void updateBattery(NotificationSetting setting, boolean lowBatteryEnabled, BatteryThreshold threshold) {
        setting.updateBattery(lowBatteryEnabled, threshold);
        notificationSettingRepository.save(setting);
    }
}
