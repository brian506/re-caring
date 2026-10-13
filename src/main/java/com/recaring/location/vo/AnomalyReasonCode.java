package com.recaring.location.vo;

import java.util.Arrays;
import java.util.Optional;

public enum AnomalyReasonCode {

    VEHICLE,
    DWELL_UNKNOWN,
    DWELL_AFTER_VEHICLE,
    DWELL_KNOWN_LONG("오래 머무름 확인", false),
    DWELL_SAFEZONE_LONG("오래 머무름 확인", false),
    OFF_ROUTE,
    OUTSIDE_SAFEZONE,
    UNFAMILIAR_AREA,
    NIGHT_OUTING,
    RARE_HOUR,
    UNUSUAL_HOUR_AT_PLACE,
    NO_ARRIVAL;

    private final String notificationTitle;
    private final boolean remindPushed;

    AnomalyReasonCode() {
        this(null, true);
    }

    // 아는 장소·안심존 체류는 같은 자리에 있는 한 사건이 닫히지 않아, 재알림을 보내면 확인할 때까지 반복된다.
    AnomalyReasonCode(String notificationTitle, boolean remindPushed) {
        this.notificationTitle = notificationTitle;
        this.remindPushed = remindPushed;
    }

    public Optional<String> notificationTitle() {
        return Optional.ofNullable(notificationTitle);
    }

    public boolean isRemindPushed() {
        return remindPushed;
    }

    public static Optional<AnomalyReasonCode> find(String value) {
        return Arrays.stream(values())
                .filter(code -> code.name().equals(value))
                .findFirst();
    }
}
