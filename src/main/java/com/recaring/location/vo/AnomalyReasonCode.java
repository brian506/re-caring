package com.recaring.location.vo;

import java.util.Arrays;
import java.util.Optional;

public enum AnomalyReasonCode {

    VEHICLE(null, "차량 이동 계속 중", true),
    DWELL_UNKNOWN,
    DWELL_AFTER_VEHICLE,
    DWELL_KNOWN_LONG("오래 머무름 확인", null, false),
    DWELL_SAFEZONE_LONG(null, null, false, false),
    OFF_ROUTE,
    OUTSIDE_SAFEZONE,
    UNFAMILIAR_AREA,
    NIGHT_OUTING,
    RARE_HOUR,
    UNUSUAL_HOUR_AT_PLACE,
    NO_ARRIVAL;

    private final String notificationTitle;
    private final String remindTitle;
    private final boolean remindPushed;
    private final boolean pushed;

    AnomalyReasonCode() {
        this(null, null, true);
    }

    AnomalyReasonCode(String notificationTitle, String remindTitle, boolean remindPushed) {
        this(notificationTitle, remindTitle, remindPushed, true);
    }

    // 아는 장소·안심존 체류는 같은 자리에 있는 한 사건이 닫히지 않아, 재알림을 보내면 확인할 때까지 반복된다.
    // 안심존 체류는 실내 GPS 오차로 평소 생활과 구분되지 않아 저장만 하고 푸시하지 않는다.
    AnomalyReasonCode(String notificationTitle, String remindTitle, boolean remindPushed, boolean pushed) {
        this.notificationTitle = notificationTitle;
        this.remindTitle = remindTitle;
        this.remindPushed = remindPushed;
        this.pushed = pushed;
    }

    public Optional<String> notificationTitle(boolean remind) {
        if (remind && remindTitle != null) {
            return Optional.of(remindTitle);
        }
        return Optional.ofNullable(notificationTitle);
    }

    public boolean isRemindPushed() {
        return remindPushed;
    }

    public boolean isPushed() {
        return pushed;
    }

    public static Optional<AnomalyReasonCode> find(String value) {
        return Arrays.stream(values())
                .filter(code -> code.name().equals(value))
                .findFirst();
    }
}
