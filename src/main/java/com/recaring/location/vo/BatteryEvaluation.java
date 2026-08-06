package com.recaring.location.vo;

/**
 * reachedThreshold가 null이면 알릴 것이 없다는 뜻이며, newState는 그와 무관하게 항상 저장한다.
 */
public record BatteryEvaluation(Integer reachedThreshold, BatteryAlertState newState) {

    public static BatteryEvaluation silent(BatteryAlertState newState) {
        return new BatteryEvaluation(null, newState);
    }

    public static BatteryEvaluation alert(int reachedThreshold) {
        return new BatteryEvaluation(reachedThreshold, new BatteryAlertState(reachedThreshold));
    }

    public boolean shouldNotify() {
        return reachedThreshold != null;
    }
}
