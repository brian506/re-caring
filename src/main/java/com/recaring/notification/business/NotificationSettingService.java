package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateBatteryNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateEmergencyCallNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateSafeZoneNotificationSettingCommand;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.implement.NotificationSettingManager;
import com.recaring.notification.implement.NotificationSettingReader;
import com.recaring.notification.implement.NotificationSettingValidator;
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
    public void updateSafeZone(String requesterKey, UpdateSafeZoneNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        NotificationSetting setting = findOrAddDefault(command.wardKey());
        notificationSettingManager.updateSafeZone(
                setting,
                command.entryEnabled(),
                command.exitEnabled()
        );
    }

    @Transactional
    public void updateAnomaly(String requesterKey, UpdateAnomalyNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        NotificationSetting setting = findOrAddDefault(command.wardKey());
        notificationSettingManager.updateAnomaly(
                setting,
                command.routeDeviationEnabled(),
                command.speedAnomalyEnabled(),
                command.wanderingAnomalyEnabled(),
                command.sensitivity()
        );
    }

    @Transactional
    public void updateEmergencyCall(String requesterKey, UpdateEmergencyCallNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        NotificationSetting setting = findOrAddDefault(command.wardKey());
        notificationSettingManager.updateEmergencyCall(setting, command.enabled());
    }

    @Transactional
    public void updateBattery(String requesterKey, UpdateBatteryNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        NotificationSetting setting = findOrAddDefault(command.wardKey());
        notificationSettingManager.updateBattery(
                setting,
                command.lowBatteryEnabled(),
                command.threshold()
        );
    }

    private NotificationSetting findOrAddDefault(String wardKey) {
        notificationSettingManager.addDefaultIfAbsent(wardKey);
        return notificationSettingReader.findExistingSetting(wardKey);
    }
}
