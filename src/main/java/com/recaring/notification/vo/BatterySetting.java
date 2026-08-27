package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.NotificationSetting;

import java.util.List;

public record BatterySetting(
        boolean lowBatteryEnabled,
        List<Integer> thresholdPercents,
        List<Integer> thresholdOptions
) {
    public static BatterySetting from(NotificationSetting setting) {
        return new BatterySetting(
                setting.isLowBatteryEnabled(),
                BatteryThresholds.parse(setting.getBatteryThresholdPercents()).percents(),
                BatteryThreshold.options()
        );
    }
}
