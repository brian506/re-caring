package com.recaring.location.vo;

/**
 * 배터리 재알림 억제 상태. lastNotifiedThreshold는 마지막으로 알린 선택값이며,
 * null이면 도달한 선택값이 없어 억제할 것도 없는 상태다.
 */
public record BatteryAlertState(Integer lastNotifiedThreshold) {

    public static BatteryAlertState empty() {
        return new BatteryAlertState(null);
    }
}
