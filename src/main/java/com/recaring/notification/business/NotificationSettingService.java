package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.implement.setting.NotificationSettingManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.vo.NotificationSettings;
import com.recaring.notification.implement.setting.NotificationSettingValidator;
import com.recaring.notification.vo.BatteryThresholds;
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
    public NotificationSettings getSetting(String requesterKey, String wardKey) {
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
                command.speedAnomalyEnabled(),
                command.wanderingAnomalyEnabled(),
                command.abnormalDwellingEnabled(),
                command.routeDeviationEnabled(),
                command.timeAnomalyEnabled()
        );
    }

    @Transactional
    public void updateEmergencyCall(String requesterKey, String wardKey, boolean enabled) {
        notificationSettingValidator.validateSettingAccess(requesterKey, wardKey);
        notificationSettingManager.updateEmergencyCall(wardKey, enabled);
    }

    @Transactional
    public void updateBattery(String requesterKey, String wardKey, boolean lowBatteryEnabled, BatteryThresholds thresholds) {
        notificationSettingValidator.validateSettingAccess(requesterKey, wardKey);
        notificationSettingManager.updateBattery(wardKey, lowBatteryEnabled, thresholds);
    }
}
