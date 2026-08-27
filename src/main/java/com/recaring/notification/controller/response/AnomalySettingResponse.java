package com.recaring.notification.controller.response;

import com.recaring.notification.vo.AnomalySetting;

public record AnomalySettingResponse(
        boolean speedAnomalyEnabled,
        boolean wanderingAnomalyEnabled,
        boolean abnormalDwellingEnabled,
        boolean routeDeviationEnabled,
        boolean timeAnomalyEnabled
) {
    public static AnomalySettingResponse from(AnomalySetting setting) {
        return new AnomalySettingResponse(
                setting.speedAnomalyEnabled(),
                setting.wanderingAnomalyEnabled(),
                setting.abnormalDwellingEnabled(),
                setting.routeDeviationEnabled(),
                setting.timeAnomalyEnabled()
        );
    }
}
