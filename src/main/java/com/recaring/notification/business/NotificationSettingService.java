package com.recaring.notification.business;

import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.notification.business.command.UpdateAnomalyNotificationSettingCommand;
import com.recaring.notification.implement.setting.NotificationSettingManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.vo.NotificationSettings;
import com.recaring.notification.vo.BatteryThresholds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingReader notificationSettingReader;
    private final NotificationSettingManager notificationSettingManager;
    private final CareRelationshipValidator careRelationshipValidator;

    @Transactional(readOnly = true)
    public NotificationSettings getSetting(String requesterKey, String wardKey) {
        careRelationshipValidator.validateCaregiver(requesterKey, wardKey);
        return notificationSettingReader.findSetting(wardKey, requesterKey);
    }

    @Transactional
    public void updateSafeZone(String requesterKey, String wardKey, boolean entryEnabled, boolean exitEnabled) {
        careRelationshipValidator.validateCaregiver(requesterKey, wardKey);
        notificationSettingManager.updateSafeZone(wardKey, requesterKey, entryEnabled, exitEnabled);
    }

    @Transactional
    public void updateDangerZone(String requesterKey, String wardKey, boolean entryEnabled, boolean exitEnabled) {
        careRelationshipValidator.validateCaregiver(requesterKey, wardKey);
        notificationSettingManager.updateDangerZone(wardKey, requesterKey, entryEnabled, exitEnabled);
    }

    @Transactional
    public void updateAnomaly(String requesterKey, UpdateAnomalyNotificationSettingCommand command) {
        careRelationshipValidator.validateCaregiver(requesterKey, command.wardKey());
        notificationSettingManager.updateAnomaly(
                command.wardKey(),
                requesterKey,
                command.speedAnomalyEnabled(),
                command.wanderingAnomalyEnabled(),
                command.abnormalDwellingEnabled(),
                command.routeDeviationEnabled(),
                command.timeAnomalyEnabled()
        );
    }

    @Transactional
    public void updateEmergencyCall(String requesterKey, String wardKey, boolean enabled) {
        careRelationshipValidator.validateCaregiver(requesterKey, wardKey);
        notificationSettingManager.updateEmergencyCall(wardKey, requesterKey, enabled);
    }

    @Transactional
    public void updateBattery(String requesterKey, String wardKey, boolean lowBatteryEnabled, BatteryThresholds thresholds) {
        careRelationshipValidator.validateCaregiver(requesterKey, wardKey);
        notificationSettingManager.updateBattery(wardKey, requesterKey, lowBatteryEnabled, thresholds);
    }
}
