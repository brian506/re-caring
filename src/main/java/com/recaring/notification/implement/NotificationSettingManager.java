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
    public void updateSafeZone(String wardKey, boolean entryEnabled, boolean exitEnabled) {
        NotificationSetting setting = findOrCreate(wardKey);
        setting.updateSafeZone(entryEnabled, exitEnabled);
        notificationSettingRepository.save(setting);
    }

    @Transactional
    public void updateAnomaly(
            String wardKey,
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            AnomalySensitivity sensitivity
    ) {
        NotificationSetting setting = findOrCreate(wardKey);
        setting.updateAnomaly(routeDeviationEnabled, speedAnomalyEnabled, wanderingAnomalyEnabled, sensitivity);
        notificationSettingRepository.save(setting);
    }

    @Transactional
    public void updateEmergencyCall(String wardKey, boolean enabled) {
        NotificationSetting setting = findOrCreate(wardKey);
        setting.updateEmergencyCall(enabled);
        notificationSettingRepository.save(setting);
    }

    @Transactional
    public void updateBattery(String wardKey, boolean lowBatteryEnabled, BatteryThreshold threshold) {
        NotificationSetting setting = findOrCreate(wardKey);
        setting.updateBattery(lowBatteryEnabled, threshold);
        notificationSettingRepository.save(setting);
    }

    private NotificationSetting findOrCreate(String wardKey) {
        return notificationSettingRepository.findByWardMemberKey(wardKey)
                .orElseGet(() -> NotificationSetting.defaultFor(wardKey));
    }
}
