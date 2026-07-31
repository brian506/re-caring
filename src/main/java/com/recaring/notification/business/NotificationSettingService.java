package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.implement.NotificationSettingManager;
import com.recaring.notification.implement.NotificationSettingReader;
import com.recaring.notification.implement.NotificationSettingValidator;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.BatteryThresholdRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingReader notificationSettingReader;
    private final NotificationSettingManager notificationSettingManager;
    private final NotificationSettingValidator notificationSettingValidator;

    @Transactional(readOnly = true)
    public NotificationSettingInfo getSetting(String requesterKey, String wardKey) {
        notificationSettingValidator.validateSettingAccess(requesterKey, wardKey);
        return notificationSettingReader.findSetting(wardKey);
    }

    @Transactional
    public void updateSafeZone(String requesterKey, String wardKey, boolean entryEnabled, boolean exitEnabled) {
        notificationSettingValidator.validateSettingAccess(requesterKey, wardKey);
        notificationSettingManager.updateSafeZone(wardKey, entryEnabled, exitEnabled);
    }

    @Transactional
    public void updateAnomaly(String requesterKey, UpdateAnomalyNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        notificationSettingManager.updateAnomaly(
                command.wardKey(),
                command.routeDeviationEnabled(),
                command.speedAnomalyEnabled(),
                command.wanderingAnomalyEnabled(),
                command.routeDeviationSensitivity(),
                command.speedAnomalySensitivity(),
                command.wanderingAnomalySensitivity()
        );
    }

    @Transactional
    public void updateEmergencyCall(String requesterKey, String wardKey, boolean enabled) {
        notificationSettingValidator.validateSettingAccess(requesterKey, wardKey);
        notificationSettingManager.updateEmergencyCall(wardKey, enabled);
    }

    @Transactional
    public void updateBattery(String requesterKey, String wardKey, boolean lowBatteryEnabled, BatteryThresholdRange thresholdRange) {
        notificationSettingValidator.validateSettingAccess(requesterKey, wardKey);
        notificationSettingManager.updateBattery(wardKey, lowBatteryEnabled, thresholdRange);
    }
}
