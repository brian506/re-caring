package com.recaring.notification.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("BatteryThreshold 단위 테스트")
class BatteryThresholdTest {

    @ParameterizedTest
    @ValueSource(ints = {10, 20, 50, 100})
    @DisplayName("10~100 사이 10 단위 값은 허용한다")
    void accepts_supported_percent(int percent) {
        assertThatCode(() -> new BatteryThreshold(percent)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, 15, 101, 110, -10})
    @DisplayName("10 단위가 아니거나 범위를 벗어나면 예외가 발생한다")
    void rejects_unsupported_percent(int percent) {
        assertThatThrownBy(() -> new BatteryThreshold(percent))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_BATTERY_THRESHOLD);
    }

    @Test
    @DisplayName("선택 가능한 값으로 10부터 100까지 10 단위 목록을 제공한다")
    void options_returns_ten_step_percents() {
        assertThat(BatteryThreshold.options())
                .containsExactly(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
    }
}
