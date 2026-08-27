package com.recaring.location.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("이상탐지 유형 단위 테스트")
class DetectionTypeTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(DetectionType.class)
    @DisplayName("합의된 탐지 유형 5종은 이름으로 찾을 수 있다")
    void finds_every_agreed_detection_type(DetectionType detectionType) {
        // when
        Optional<DetectionType> result = DetectionType.find(detectionType.name());

        assertThat(result).contains(detectionType);
    }

    @Test
    @DisplayName("협의되지 않은 유형은 찾지 못한다")
    void does_not_find_unagreed_type() {
        // when
        Optional<DetectionType> result = DetectionType.find("SIGNAL_LOST");

        assertThat(result).isEmpty();
    }

    @ParameterizedTest(name = "[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "wandering", "Wandering", "WANDERING "})
    @DisplayName("이름이 정확히 일치하지 않으면 찾지 못한다")
    void does_not_find_when_name_does_not_match_exactly(String value) {
        // when
        Optional<DetectionType> result = DetectionType.find(value);

        assertThat(result).isEmpty();
    }
}
