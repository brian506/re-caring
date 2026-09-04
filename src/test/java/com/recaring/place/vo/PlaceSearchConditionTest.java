package com.recaring.place.vo;

import com.recaring.place.fixture.PlaceFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaceSearchCondition 단위 테스트")
class PlaceSearchConditionTest {

    private static final String QUERY = PlaceFixture.NEARBY_KEYWORD;
    private static final double LATITUDE = PlaceFixture.SEOUL_LATITUDE;
    private static final double LONGITUDE = PlaceFixture.SEOUL_LONGITUDE;

    private static final int KAKAO_MAX_RADIUS_METERS = 20_000;

    @Test
    @DisplayName("반경을 지정하지 않으면 카카오 상한인 20000m로 검색한다")
    void radius_defaults_to_kakao_limit() {
        PlaceSearchCondition condition = PlaceSearchCondition.of(QUERY, null, null, null);

        assertThat(condition.radiusMeters()).isEqualTo(KAKAO_MAX_RADIUS_METERS);
    }

    @ParameterizedTest
    @CsvSource({
            "20001, 20000",
            "30000, 20000",
            "20000, 20000",
            "-1, 0"
    })
    @DisplayName("반경은 카카오가 허용하는 0~20000m 범위로 잘린다")
    void radius_is_clamped_to_kakao_range(int requested, int expected) {
        PlaceSearchCondition condition = PlaceSearchCondition.of(QUERY, null, null, requested);

        assertThat(condition.radiusMeters()).isEqualTo(expected);
    }

    @Test
    @DisplayName("위도와 경도가 모두 있으면 그 좌표를 중심으로 편향 검색한다")
    void bias_applies_when_both_coordinates_given() {
        PlaceSearchCondition condition = PlaceSearchCondition.of(QUERY, LATITUDE, LONGITUDE, null);

        assertThat(condition.hasBias()).isTrue();
        assertThat(condition.latitude()).isEqualTo(LATITUDE);
        assertThat(condition.longitude()).isEqualTo(LONGITUDE);
    }

    @ParameterizedTest
    @CsvSource({
            "37.55, ",
            ", 126.91"
    })
    @DisplayName("위도와 경도 중 하나만 오면 편향 없이 검색한다")
    void bias_ignored_when_only_one_coordinate_given(Double latitude, Double longitude) {
        PlaceSearchCondition condition = PlaceSearchCondition.of(QUERY, latitude, longitude, null);

        assertThat(condition.hasBias()).isFalse();
        assertThat(condition.latitude()).isNull();
        assertThat(condition.longitude()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "90.0, 180.0",
            "-90.0, -180.0"
    })
    @DisplayName("위경도 범위의 경계값 좌표는 편향 검색에 그대로 쓴다")
    void bias_applies_on_coordinate_range_boundary(double latitude, double longitude) {
        PlaceSearchCondition condition = PlaceSearchCondition.of(QUERY, latitude, longitude, null);

        assertThat(condition.hasBias()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "90.1, 126.91",
            "37.55, 180.1",
            "-90.1, 126.91",
            "37.55, -180.1"
    })
    @DisplayName("좌표가 위경도 범위를 벗어나면 편향 없이 검색한다")
    void bias_ignored_when_coordinates_out_of_range(double latitude, double longitude) {
        PlaceSearchCondition condition = PlaceSearchCondition.of(QUERY, latitude, longitude, null);

        assertThat(condition.hasBias()).isFalse();
    }

    @Test
    @DisplayName("편향을 제거해도 검색어와 반경은 그대로 유지된다")
    void without_bias_keeps_keyword_and_radius() {
        PlaceSearchCondition condition = PlaceSearchCondition.of(QUERY, LATITUDE, LONGITUDE, 5_000);

        PlaceSearchCondition result = condition.withoutBias();

        assertThat(result.hasBias()).isFalse();
        assertThat(result.keyword()).isEqualTo(condition.keyword());
        assertThat(result.radiusMeters()).isEqualTo(5_000);
    }
}
