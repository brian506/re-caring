package com.recaring.notification.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.List;
import java.util.stream.IntStream;

public record BatteryThreshold(int percent) {

    public static final int MIN_PERCENT = 10;
    public static final int MAX_PERCENT = 100;
    public static final int STEP_PERCENT = 5;
    public static final BatteryThreshold DEFAULT = new BatteryThreshold(25);

    public BatteryThreshold {
        if (!isSupported(percent)) {
            throw new AppException(ErrorType.INVALID_BATTERY_THRESHOLD);
        }
    }

    public static List<Integer> options() {
        return IntStream.iterate(MIN_PERCENT, value -> value <= MAX_PERCENT, value -> value + STEP_PERCENT)
                .boxed()
                .toList();
    }

    private static boolean isSupported(int percent) {
        return percent >= MIN_PERCENT
                && percent <= MAX_PERCENT
                && percent % STEP_PERCENT == 0;
    }
}
