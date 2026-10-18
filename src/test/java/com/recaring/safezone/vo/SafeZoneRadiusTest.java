package com.recaring.safezone.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SafeZoneRadius VO 단위 테스트")
class SafeZoneRadiusTest {

    @ParameterizedTest
    @ValueSource(ints = {300, 2000})
    @DisplayName("반경 300m와 2000m는 허용 범위 경계라 생성된다")
    void create_success_at_boundaries(int meters) {
        SafeZoneRadius radius = new SafeZoneRadius(meters);

        assertThat(radius.meters()).isEqualTo(meters);
    }

    @ParameterizedTest
    @ValueSource(ints = {299, 2001})
    @DisplayName("반경이 300m 미만이거나 2000m 초과면 INVALID_SAFE_ZONE_RADIUS 예외가 발생한다")
    void create_fail_outside_boundaries(int meters) {
        assertThatThrownBy(() -> new SafeZoneRadius(meters))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_SAFE_ZONE_RADIUS);
    }
}
