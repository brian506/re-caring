package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.time.LocalDateTime;

import static com.recaring.notification.dataaccess.entity.QNotificationSetting.notificationSetting;

public class NotificationSettingRepositoryCustomImpl extends QuerydslRepositorySupport
        implements NotificationSettingRepositoryCustom {

    protected NotificationSettingRepositoryCustomImpl() {
        super(NotificationSetting.class);
    }

    @Override
    public void deleteByWardMemberKey(String wardMemberKey) {
        delete(notificationSetting)
                .where(notificationSetting.wardMemberKey.eq(wardMemberKey))
                .execute();
    }

    @Override
    public void updateSafeZone(String wardMemberKey, boolean entryEnabled, boolean exitEnabled) {
        update(notificationSetting)
                .set(notificationSetting.safeZoneEntryEnabled, entryEnabled)
                .set(notificationSetting.safeZoneExitEnabled, exitEnabled)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(notificationSetting.wardMemberKey.eq(wardMemberKey))
                .execute();
    }

    @Override
    public void updateAnomaly(
            String wardMemberKey,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    ) {
        update(notificationSetting)
                .set(notificationSetting.speedAnomalyEnabled, speedAnomalyEnabled)
                .set(notificationSetting.wanderingAnomalyEnabled, wanderingAnomalyEnabled)
                .set(notificationSetting.abnormalDwellingEnabled, abnormalDwellingEnabled)
                .set(notificationSetting.routeDeviationEnabled, routeDeviationEnabled)
                .set(notificationSetting.timeAnomalyEnabled, timeAnomalyEnabled)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(notificationSetting.wardMemberKey.eq(wardMemberKey))
                .execute();
    }

    @Override
    public void updateEmergencyCall(String wardMemberKey, boolean enabled) {
        update(notificationSetting)
                .set(notificationSetting.emergencyCallEnabled, enabled)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(notificationSetting.wardMemberKey.eq(wardMemberKey))
                .execute();
    }

    @Override
    public void updateBattery(String wardMemberKey, boolean lowBatteryEnabled, String batteryThresholdPercents) {
        update(notificationSetting)
                .set(notificationSetting.lowBatteryEnabled, lowBatteryEnabled)
                .set(notificationSetting.batteryThresholdPercents, batteryThresholdPercents)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(notificationSetting.wardMemberKey.eq(wardMemberKey))
                .execute();
    }
}
