package com.recaring.notification.vo;

/**
 * 배터리 알림 판정에 필요한 설정. 마스터 스위치와 고른 잔량(%) 목록을 한 번에 담아
 * 판정 경로가 설정 조회를 두 번 하지 않도록 한다.
 */
public record BatteryNotificationSetting(boolean enabled, BatteryThresholds thresholds) {

    public static final BatteryNotificationSetting DEFAULT =
            new BatteryNotificationSetting(true, BatteryThresholds.NONE);

    public BatteryNotificationSetting {
        if (thresholds == null) {
            thresholds = BatteryThresholds.NONE;
        }
    }

    public boolean hasNoThreshold() {
        return thresholds.isEmpty();
    }
}
