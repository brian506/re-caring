package com.recaring.notification.vo;

import java.util.Arrays;
import java.util.Optional;

public enum SafeZoneAlertType {

    SAFE_ZONE_ENTERED("안심존 진입 알림"),
    SAFE_ZONE_EXITED("안심존 이탈 알림");

    private final String notificationTitle;

    SafeZoneAlertType(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String notificationTitle() {
        return notificationTitle;
    }

    public boolean isEnabledIn(SafeZoneSetting setting) {
        return this == SAFE_ZONE_ENTERED ? setting.entryEnabled() : setting.exitEnabled();
    }

    public static Optional<SafeZoneAlertType> find(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(value))
                .findFirst();
    }
}
