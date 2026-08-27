package com.recaring.notification.controller.response;

import com.recaring.notification.vo.BatterySetting;

import java.util.List;

public record BatterySettingResponse(
        boolean lowBatteryEnabled,
        List<Integer> thresholdPercents,
        List<Integer> thresholdOptions
) {
    public static BatterySettingResponse from(BatterySetting setting) {
        return new BatterySettingResponse(
                setting.lowBatteryEnabled(),
                setting.thresholdPercents(),
                setting.thresholdOptions()
        );
    }
}
