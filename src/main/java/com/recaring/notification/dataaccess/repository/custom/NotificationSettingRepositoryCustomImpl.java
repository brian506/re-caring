package com.recaring.notification.dataaccess.repository.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
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
    public void deleteByMemberKey(String memberKey) {
        delete(notificationSetting)
                .where(notificationSetting.wardMemberKey.eq(memberKey)
                        .or(notificationSetting.caregiverMemberKey.eq(memberKey)))
                .execute();
    }

    @Override
    public void deleteByRelationship(String wardMemberKey, String caregiverMemberKey) {
        delete(notificationSetting)
                .where(ownedBy(wardMemberKey, caregiverMemberKey))
                .execute();
    }

    @Override
    public void updateSafeZone(String wardMemberKey, String caregiverMemberKey, boolean entryEnabled, boolean exitEnabled) {
        update(notificationSetting)
                .set(notificationSetting.safeZoneEntryEnabled, entryEnabled)
                .set(notificationSetting.safeZoneExitEnabled, exitEnabled)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(ownedBy(wardMemberKey, caregiverMemberKey))
                .execute();
    }

    @Override
    public void updateDangerZone(String wardMemberKey, String caregiverMemberKey, boolean entryEnabled, boolean exitEnabled) {
        update(notificationSetting)
                .set(notificationSetting.dangerZoneEntryEnabled, entryEnabled)
                .set(notificationSetting.dangerZoneExitEnabled, exitEnabled)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(ownedBy(wardMemberKey, caregiverMemberKey))
                .execute();
    }

    @Override
    public void updateAnomaly(
            String wardMemberKey,
            String caregiverMemberKey,
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
                .where(ownedBy(wardMemberKey, caregiverMemberKey))
                .execute();
    }

    @Override
    public void updateEmergencyCall(String wardMemberKey, String caregiverMemberKey, boolean enabled) {
        update(notificationSetting)
                .set(notificationSetting.emergencyCallEnabled, enabled)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(ownedBy(wardMemberKey, caregiverMemberKey))
                .execute();
    }

    @Override
    public void updateBattery(
            String wardMemberKey,
            String caregiverMemberKey,
            boolean lowBatteryEnabled,
            String batteryThresholdPercents
    ) {
        update(notificationSetting)
                .set(notificationSetting.lowBatteryEnabled, lowBatteryEnabled)
                .set(notificationSetting.batteryThresholdPercents, batteryThresholdPercents)
                .set(notificationSetting.updatedAt, LocalDateTime.now())
                .where(ownedBy(wardMemberKey, caregiverMemberKey))
                .execute();
    }

    private BooleanExpression ownedBy(String wardMemberKey, String caregiverMemberKey) {
        return notificationSetting.wardMemberKey.eq(wardMemberKey)
                .and(notificationSetting.caregiverMemberKey.eq(caregiverMemberKey));
    }
}
