package com.recaring.notification.vo;

import com.recaring.safezone.vo.SafeZoneType;

import java.util.Arrays;
import java.util.Optional;

public enum SafeZoneAlertType {

    SAFE_ZONE_ENTERED("안심존 진입 알림", "%s에 도착했어요."),
    SAFE_ZONE_EXITED("안심존 이탈 알림", "%s에서 벗어났어요."),
    DANGER_ZONE_ENTERED("위험존 진입 알림", "위험존 %s에 들어갔어요."),
    DANGER_ZONE_EXITED("위험존 이탈 알림", "위험존 %s에서 벗어났어요.");

    private final String notificationTitle;
    private final String bodySuffixFormat;

    SafeZoneAlertType(String notificationTitle, String bodySuffixFormat) {
        this.notificationTitle = notificationTitle;
        this.bodySuffixFormat = bodySuffixFormat;
    }

    public static SafeZoneAlertType entered(SafeZoneType zoneType) {
        return zoneType == SafeZoneType.DANGER ? DANGER_ZONE_ENTERED : SAFE_ZONE_ENTERED;
    }

    public static SafeZoneAlertType exited(SafeZoneType zoneType) {
        return zoneType == SafeZoneType.DANGER ? DANGER_ZONE_EXITED : SAFE_ZONE_EXITED;
    }

    public String notificationTitle() {
        return notificationTitle;
    }

    public String bodySuffix(String zoneName) {
        return bodySuffixFormat.formatted(zoneName);
    }

    public boolean isEnabledIn(NotificationSettings settings) {
        return switch (this) {
            case SAFE_ZONE_ENTERED -> settings.safeZone().entryEnabled();
            case SAFE_ZONE_EXITED -> settings.safeZone().exitEnabled();
            case DANGER_ZONE_ENTERED -> settings.dangerZone().entryEnabled();
            case DANGER_ZONE_EXITED -> settings.dangerZone().exitEnabled();
        };
    }

    public static Optional<SafeZoneAlertType> find(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(value))
                .findFirst();
    }
}
