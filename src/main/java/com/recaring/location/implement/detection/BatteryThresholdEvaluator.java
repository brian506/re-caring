package com.recaring.location.implement.detection;

import com.recaring.location.vo.BatteryAlertState;
import com.recaring.location.vo.BatteryEvaluation;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 사용자가 고른 잔량(%) 중 현재 배터리가 도달한 가장 낮은 값을 찾아 알림 여부를 판정한다.
 * 배터리 잔량은 부하·온도로 1~2% 흔들리므로, 한 번 알린 뒤에는 그 값보다 5% 넘게 회복해야
 * 다시 무장한다. 그 전까지는 잔량이 임계값 위로 잠깐 튀어도 상태를 유지해 재알림을 막는다.
 */
@Component
public class BatteryThresholdEvaluator {

    private static final int REARM_MARGIN_PERCENT = 5;

    public BatteryEvaluation evaluate(int batteryPercent, List<Integer> thresholdPercents, Integer lastNotifiedThreshold) {
        Integer reached = findLowestReached(batteryPercent, thresholdPercents);

        if (lastNotifiedThreshold == null) {
            return reached == null
                    ? BatteryEvaluation.silent(BatteryAlertState.empty())
                    : BatteryEvaluation.alert(reached);
        }

        // 충분히 회복했으면 알리지 않고 현재 구간으로 무장만 옮긴다. 충전 중 중간 임계값에서 오알림이 나가지 않는다.
        if (batteryPercent >= lastNotifiedThreshold + REARM_MARGIN_PERCENT) {
            return BatteryEvaluation.silent(new BatteryAlertState(reached));
        }

        if (reached == null || reached >= lastNotifiedThreshold) {
            return BatteryEvaluation.silent(new BatteryAlertState(lastNotifiedThreshold));
        }

        return BatteryEvaluation.alert(reached);
    }

    private Integer findLowestReached(int batteryPercent, List<Integer> thresholdPercents) {
        return thresholdPercents.stream()
                .filter(threshold -> batteryPercent <= threshold)
                .min(Integer::compareTo)
                .orElse(null);
    }
}
