package com.recaring.notification.controller.request;

import com.recaring.notification.vo.BatteryThresholds;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateBatteryNotificationSettingRequest(
        @NotNull Boolean lowBatteryEnabled,

        @NotNull
        @Schema(description = "알림 받을 잔량(%) 목록. 빈 배열이면 배터리 알림을 보내지 않는다", example = "[20, 50]")
        List<Integer> thresholdPercents
) {
    public BatteryThresholds toThresholds() {
        return BatteryThresholds.ofPercents(thresholdPercents);
    }
}
