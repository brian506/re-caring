package com.recaring.location.vo;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("위치 수집 주기 단위 테스트")
class LocationCollectionIntervalTest {

    @Test
    @DisplayName("허용된 초 값을 위치 수집 주기로 변환한다")
    void fromSeconds_returns_interval_for_allowed_value() {
        LocationCollectionInterval result = LocationCollectionInterval.fromSeconds(30);

        assertThat(result).isEqualTo(LocationCollectionInterval.THIRTY_SECONDS);
        assertThat(result.seconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("허용된 주기 옵션을 초 단위 목록으로 반환한다")
    void options_returns_allowed_seconds() {
        assertThat(LocationCollectionInterval.options())
                .containsExactly(5, 10, 30, 60, 180, 300);
    }

    @Test
    @DisplayName("허용되지 않은 초 값이면 예외가 발생한다")
    void fromSeconds_throws_for_unsupported_value() {
        assertThatThrownBy(() -> LocationCollectionInterval.fromSeconds(120))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_LOCATION_COLLECTION_INTERVAL);
    }
}
