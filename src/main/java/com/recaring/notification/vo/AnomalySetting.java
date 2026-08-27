package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.NotificationSetting;

public record AnomalySetting(
        boolean speedAnomalyEnabled,
        boolean wanderingAnomalyEnabled,
        boolean abnormalDwellingEnabled,
        boolean routeDeviationEnabled,
        boolean timeAnomalyEnabled
) {
    public static AnomalySetting from(NotificationSetting setting) {
        return new AnomalySetting(
                setting.isSpeedAnomalyEnabled(),
                setting.isWanderingAnomalyEnabled(),
                setting.isAbnormalDwellingEnabled(),
                setting.isRouteDeviationEnabled(),
                setting.isTimeAnomalyEnabled()
        );
    }
}
