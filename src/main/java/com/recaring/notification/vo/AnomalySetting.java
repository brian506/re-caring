package com.recaring.notification.vo;

import com.recaring.location.vo.DetectionType;
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

    public boolean isEnabled(DetectionType detectionType) {
        return switch (detectionType) {
            case SPEED_ANOMALY -> speedAnomalyEnabled;
            case WANDERING -> wanderingAnomalyEnabled;
            case ABNORMAL_DWELLING -> abnormalDwellingEnabled;
            case ROUTE_DEVIATION -> routeDeviationEnabled;
            case TIME_ANOMALY -> timeAnomalyEnabled;
        };
    }
}
