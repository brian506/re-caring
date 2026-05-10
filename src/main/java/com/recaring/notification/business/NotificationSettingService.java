package com.recaring.notification.business;

import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateBatteryNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateEmergencyCallNotificationSettingCommand;
import com.recaring.notification.business.command.UpdateSafeZoneNotificationSettingCommand;
import com.recaring.notification.implement.NotificationSettingManager;
import com.recaring.notification.implement.NotificationSettingReader;
import com.recaring.notification.implement.NotificationSettingValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingReader notificationSettingReader;
    private final NotificationSettingManager notificationSettingManager;
    private final NotificationSettingValidator notificationSettingValidator;

    public NotificationSettingInfo getSetting(String requesterKey, String wardKey) {
        notificationSettingValidator.validateSettingAccess(requesterKey, wardKey);
        return notificationSettingReader.findSetting(wardKey);
    }

    public void updateSafeZone(String requesterKey, UpdateSafeZoneNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        notificationSettingManager.updateSafeZone(
                command.wardKey(),
                command.entryEnabled(),
                command.exitEnabled()
        );
    }

    public void updateAnomaly(String requesterKey, UpdateAnomalyNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        notificationSettingManager.updateAnomaly(
                command.wardKey(),
                command.routeDeviationEnabled(),
                command.speedAnomalyEnabled(),
                command.wanderingAnomalyEnabled(),
                command.sensitivity()
        );
    }

    public void updateEmergencyCall(String requesterKey, UpdateEmergencyCallNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        notificationSettingManager.updateEmergencyCall(command.wardKey(), command.enabled());
    }

    public void updateBattery(String requesterKey, UpdateBatteryNotificationSettingCommand command) {
        notificationSettingValidator.validateSettingAccess(requesterKey, command.wardKey());
        notificationSettingManager.updateBattery(
                command.wardKey(),
                command.lowBatteryEnabled(),
                command.threshold()
        );
    }
}
