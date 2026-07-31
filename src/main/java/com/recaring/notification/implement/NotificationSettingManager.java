package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThresholdRange;
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
    public void updateSafeZone(String wardKey, boolean entryEnabled, boolean exitEnabled) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateSafeZone(wardKey, entryEnabled, exitEnabled);
    }

    @Transactional
    public void updateAnomaly(
            String wardKey,
            boolean routeDeviationEnabled,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            AnomalySensitivity routeDeviationSensitivity,
            AnomalySensitivity speedAnomalySensitivity,
            AnomalySensitivity wanderingAnomalySensitivity
    ) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateAnomaly(
                wardKey,
                routeDeviationEnabled, speedAnomalyEnabled, wanderingAnomalyEnabled,
                routeDeviationSensitivity, speedAnomalySensitivity, wanderingAnomalySensitivity);
    }

    @Transactional
    public void updateEmergencyCall(String wardKey, boolean enabled) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateEmergencyCall(wardKey, enabled);
    }

    @Transactional
    public void updateBattery(String wardKey, boolean lowBatteryEnabled, BatteryThresholdRange thresholdRange) {
        notificationSettingRepository.insertDefaultIfAbsent(wardKey);
        notificationSettingRepository.updateBattery(
                wardKey, lowBatteryEnabled, thresholdRange.low().percent(), thresholdRange.full().percent());
    }
}
