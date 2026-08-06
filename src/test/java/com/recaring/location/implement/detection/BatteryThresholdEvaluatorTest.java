package com.recaring.location.implement.detection;

import com.recaring.location.vo.BatteryAlertState;
import com.recaring.location.vo.BatteryEvaluation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BatteryThresholdEvaluator 단위 테스트")
class BatteryThresholdEvaluatorTest {

    private static final List<Integer> THRESHOLDS = List.of(20, 50);

    private final BatteryThresholdEvaluator evaluator = new BatteryThresholdEvaluator();

    @Test
    @DisplayName("고른 값에 아직 도달하지 않으면 알리지 않고 무장 해제 상태를 유지한다")
    void evaluate_stays_silent_when_no_threshold_reached() {
        BatteryEvaluation result = evaluator.evaluate(55, THRESHOLDS, null);

        assertThat(result.shouldNotify()).isFalse();
        assertThat(result.newState()).isEqualTo(BatteryAlertState.empty());
    }

    @Test
    @DisplayName("처음 고른 값에 도달하면 알리고 그 값으로 무장한다")
    void evaluate_alerts_on_first_reach() {
        BatteryEvaluation result = evaluator.evaluate(48, THRESHOLDS, null);

        assertThat(result.shouldNotify()).isTrue();
        assertThat(result.reachedThreshold()).isEqualTo(50);
        assertThat(result.newState()).isEqualTo(new BatteryAlertState(50));
    }

    @Test
    @DisplayName("여러 고른 값에 동시에 도달하면 가장 낮은 값을 기준으로 알린다")
    void evaluate_alerts_with_lowest_reached_threshold() {
        BatteryEvaluation result = evaluator.evaluate(15, THRESHOLDS, null);

        assertThat(result.reachedThreshold()).isEqualTo(20);
    }

    @Test
    @DisplayName("이미 알린 값과 같은 구간에 머물면 다시 알리지 않는다")
    void evaluate_suppresses_repeat_alert_in_same_band() {
        BatteryEvaluation result = evaluator.evaluate(44, THRESHOLDS, 50);

        assertThat(result.shouldNotify()).isFalse();
        assertThat(result.newState()).isEqualTo(new BatteryAlertState(50));
    }

    @Test
    @DisplayName("잔량이 임계값 위로 잠깐 튀어도 5% 회복 전이면 무장 상태를 유지한다")
    void evaluate_keeps_state_when_recovery_is_within_margin() {
        BatteryEvaluation result = evaluator.evaluate(54, THRESHOLDS, 50);

        assertThat(result.shouldNotify()).isFalse();
        assertThat(result.newState()).isEqualTo(new BatteryAlertState(50));
    }

    @Test
    @DisplayName("알린 값보다 5% 넘게 회복하면 알리지 않고 현재 구간으로 무장을 옮긴다")
    void evaluate_rearms_without_alert_when_recovered() {
        BatteryEvaluation result = evaluator.evaluate(26, THRESHOLDS, 20);

        assertThat(result.shouldNotify()).isFalse();
        assertThat(result.newState()).isEqualTo(new BatteryAlertState(50));
    }

    @Test
    @DisplayName("고른 값 범위를 완전히 벗어날 만큼 충전되면 무장 해제 상태로 초기화한다")
    void evaluate_disarms_when_fully_charged() {
        BatteryEvaluation result = evaluator.evaluate(100, THRESHOLDS, 20);

        assertThat(result.shouldNotify()).isFalse();
        assertThat(result.newState()).isEqualTo(BatteryAlertState.empty());
    }

    @Test
    @DisplayName("이미 알린 값보다 더 낮은 값에 도달하면 즉시 다시 알린다")
    void evaluate_alerts_again_on_lower_threshold() {
        BatteryEvaluation result = evaluator.evaluate(18, THRESHOLDS, 50);

        assertThat(result.shouldNotify()).isTrue();
        assertThat(result.reachedThreshold()).isEqualTo(20);
        assertThat(result.newState()).isEqualTo(new BatteryAlertState(20));
    }

    @Test
    @DisplayName("재무장 후 같은 값에 다시 도달하면 다시 알린다")
    void evaluate_alerts_again_after_rearm() {
        BatteryEvaluation rearmed = evaluator.evaluate(26, THRESHOLDS, 20);

        BatteryEvaluation result = evaluator.evaluate(18, THRESHOLDS, rearmed.newState().lastNotifiedThreshold());

        assertThat(result.shouldNotify()).isTrue();
        assertThat(result.reachedThreshold()).isEqualTo(20);
    }

    @Test
    @DisplayName("잔량이 고른 값과 정확히 같으면 도달한 것으로 본다")
    void evaluate_treats_exact_match_as_reached() {
        BatteryEvaluation result = evaluator.evaluate(50, THRESHOLDS, null);

        assertThat(result.shouldNotify()).isTrue();
        assertThat(result.reachedThreshold()).isEqualTo(50);
    }
}
