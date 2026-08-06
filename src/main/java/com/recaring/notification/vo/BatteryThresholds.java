package com.recaring.notification.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 사용자가 알림을 받겠다고 고른 배터리 잔량(%) 목록. 비어 있으면 고른 값이 없다는 뜻이며
 * 이 경우 배터리 알림을 보내지 않는다. 기본값은 두지 않는다.
 */
public record BatteryThresholds(List<BatteryThreshold> values) {

    public static final BatteryThresholds NONE = new BatteryThresholds(List.of());

    private static final String DELIMITER = ",";

    public BatteryThresholds {
        if (values == null) {
            values = List.of();
        }
        values = values.stream()
                .distinct()
                .sorted(Comparator.comparingInt(BatteryThreshold::percent))
                .toList(); // 불변 리스트로 생성
    }

    public static BatteryThresholds ofPercents(List<Integer> percents) {
        if (percents == null || percents.isEmpty()) {
            return NONE;
        }
        return new BatteryThresholds(percents.stream().map(BatteryThreshold::new).toList());
    }

    public static BatteryThresholds parse(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return NONE;
        }
        return ofPercents(Arrays.stream(storedValue.split(DELIMITER))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(BatteryThresholds::toPercent)
                .toList());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    // "20,50" 문자열 형식으로 DB 저장(csv 구조)
    public String format() {
        return String.join(DELIMITER, percents().stream().map(String::valueOf).toList());
    }

    public List<Integer> percents() {
        return values.stream().map(BatteryThreshold::percent).toList();
    }

    private static int toPercent(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AppException(ErrorType.INVALID_BATTERY_THRESHOLD);
        }
    }
}
