package com.recaring.notification.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BatteryThresholds 단위 테스트")
class BatteryThresholdsTest {

    @Test
    @DisplayName("중복을 제거하고 오름차순으로 정렬한다")
    void ofPercents_removes_duplicates_and_sorts() {
        BatteryThresholds thresholds = BatteryThresholds.ofPercents(List.of(50, 20, 50));

        assertThat(thresholds.percents()).containsExactly(20, 50);
    }

    @Test
    @DisplayName("고른 값이 없으면 빈 목록으로 취급한다")
    void ofPercents_returns_none_when_empty() {
        assertThat(BatteryThresholds.ofPercents(List.of()).isEmpty()).isTrue();
        assertThat(BatteryThresholds.ofPercents(null).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("저장 문자열과 목록을 왕복 변환해도 값이 유지된다")
    void parse_and_format_round_trip() {
        BatteryThresholds thresholds = BatteryThresholds.ofPercents(List.of(50, 20));

        String stored = thresholds.format();

        assertThat(stored).isEqualTo("20,50");
        assertThat(BatteryThresholds.parse(stored).percents()).containsExactly(20, 50);
    }

    @Test
    @DisplayName("저장 값이 비어 있으면 고른 값이 없는 것으로 본다")
    void parse_returns_none_when_stored_value_blank() {
        assertThat(BatteryThresholds.parse(null).isEmpty()).isTrue();
        assertThat(BatteryThresholds.parse("").isEmpty()).isTrue();
        assertThat(BatteryThresholds.parse("   ").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("저장 값의 공백은 무시하고 파싱한다")
    void parse_trims_whitespace() {
        assertThat(BatteryThresholds.parse(" 20 , 50 ").percents()).containsExactly(20, 50);
    }

    @Test
    @DisplayName("고른 값이 없으면 빈 문자열로 저장된다")
    void format_returns_empty_string_when_none() {
        assertThat(BatteryThresholds.NONE.format()).isEmpty();
    }

    @Test
    @DisplayName("저장 값에 숫자가 아닌 항목이 있으면 예외가 발생한다")
    void parse_throws_when_value_is_not_number() {
        assertThatThrownBy(() -> BatteryThresholds.parse("20,abc"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_BATTERY_THRESHOLD);
    }

    @Test
    @DisplayName("지원하지 않는 잔량이 섞여 있으면 예외가 발생한다")
    void ofPercents_throws_when_percent_unsupported() {
        assertThatThrownBy(() -> BatteryThresholds.ofPercents(List.of(25)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_BATTERY_THRESHOLD);
    }
}
