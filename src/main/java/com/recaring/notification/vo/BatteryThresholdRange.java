package com.recaring.notification.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record BatteryThresholdRange(BatteryThreshold low, BatteryThreshold full) {

    public static final BatteryThresholdRange DEFAULT =
            new BatteryThresholdRange(BatteryThreshold.DEFAULT, BatteryThreshold.DEFAULT_FULL);

    public BatteryThresholdRange {
        if (low.percent() >= full.percent()) {
            throw new AppException(ErrorType.INVALID_BATTERY_THRESHOLD);
        }
    }
}
