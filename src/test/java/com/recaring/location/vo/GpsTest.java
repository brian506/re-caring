package com.recaring.location.vo;

import com.recaring.location.fixture.LocationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static com.recaring.location.fixture.LocationFixture.JUMP_15KM_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.JUMP_45KM_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.LATITUDE;
import static com.recaring.location.fixture.LocationFixture.SPIKE_1KM_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.TRACK_ACCURACY;
import static com.recaring.location.fixture.LocationFixture.TRACK_START;
import static com.recaring.location.fixture.LocationFixture.WALK_50M_LATITUDE_DELTA;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gps 표시 가능·튐 판정 단위 테스트")
class GpsTest {

    private static final Gps ANCHOR = LocationFixture.createTrackPoint(LATITUDE, TRACK_ACCURACY, TRACK_START);
    private static final LocalDateTime THIRTY_SECONDS_LATER = TRACK_START.plusSeconds(30);
    private static final LocalDateTime ONE_MINUTE_LATER = TRACK_START.plusSeconds(60);
    private static final double JUMP_260M_LATITUDE_DELTA = 0.0023382;
    private static final double VISIT_300M_LATITUDE_DELTA = 0.002698;

    @Test
    @DisplayName("기준점이 없으면 정확한 좌표는 표시한다")
    void accurate_point_without_anchor_is_displayable() {
        Gps first = LocationFixture.createTrackPoint(LATITUDE, TRACK_ACCURACY, TRACK_START);

        boolean displayable = first.isDisplayableAfter(null);

        assertThat(displayable).isTrue();
    }

    @Test
    @DisplayName("30초 동안 50m를 걸어 이동한 좌표는 표시한다")
    void walking_point_is_displayable() {
        Gps walked = LocationFixture.createTrackPoint(
                LATITUDE + WALK_50M_LATITUDE_DELTA, TRACK_ACCURACY, THIRTY_SECONDS_LATER);

        boolean displayable = walked.isDisplayableAfter(ANCHOR);

        assertThat(displayable).isTrue();
    }

    @Test
    @DisplayName("30초 만에 15km 튄 좌표는 표시하지 않는다")
    void point_jumping_15km_in_30_seconds_is_not_displayable() {
        Gps jumped = LocationFixture.createTrackPoint(
                LATITUDE + JUMP_15KM_LATITUDE_DELTA, TRACK_ACCURACY, THIRTY_SECONDS_LATER);

        boolean displayable = jumped.isDisplayableAfter(ANCHOR);

        assertThat(displayable).isFalse();
    }

    @ParameterizedTest(name = "위도 차 {0}도(≈{1}m) / 30초 → 표시 {2}")
    @CsvSource({
            "0.01340, 1490, true",
            "0.01358, 1510, false"
    })
    @DisplayName("30초 간격에서 이동 속도가 50m/s 바로 아래면 표시하고 바로 위면 표시하지 않는다")
    void speed_limit_boundary(double latitudeDelta, int approxMeters, boolean expected) {
        Gps anchor = LocationFixture.createTrackPoint(LATITUDE, null, TRACK_START);
        Gps moved = LocationFixture.createTrackPoint(LATITUDE + latitudeDelta, null, THIRTY_SECONDS_LATER);

        boolean displayable = moved.isDisplayableAfter(anchor);

        assertThat(displayable).isEqualTo(expected);
    }

    @ParameterizedTest(name = "위도 차 {0}도(≈{1}m), 정확도 반경 50m씩 / 30초 → 표시 {2}")
    @CsvSource({
            "0.01430, 1590, true",
            "0.01448, 1610, false"
    })
    @DisplayName("두 좌표의 정확도 반경만큼은 이동 거리로 치지 않는다")
    void accuracy_radius_is_not_counted_as_travel(double latitudeDelta, int approxMeters, boolean expected) {
        Gps anchor = LocationFixture.createTrackPoint(LATITUDE, 50.0, TRACK_START);
        Gps moved = LocationFixture.createTrackPoint(LATITUDE + latitudeDelta, 50.0, THIRTY_SECONDS_LATER);

        boolean displayable = moved.isDisplayableAfter(anchor);

        assertThat(displayable).isEqualTo(expected);
    }

    @ParameterizedTest(name = "기준점 수신 후 {0}초 → 표시 {1}")
    @CsvSource({
            "600, false",
            "601, true"
    })
    @DisplayName("기준점을 받은 지 10분이 지나야 10분 안에 갈 수 없는 45km 밖 좌표도 새 기준점으로 받는다")
    void anchor_expires_after_ten_minutes(long secondsAfterAnchor, boolean expected) {
        LocalDateTime later = TRACK_START.plusSeconds(secondsAfterAnchor);
        Gps jumped = LocationFixture.createTrackPoint(LATITUDE + JUMP_45KM_LATITUDE_DELTA, TRACK_ACCURACY, later);

        boolean displayable = jumped.isDisplayableAfter(ANCHOR);

        assertThat(displayable).isEqualTo(expected);
    }

    @Test
    @DisplayName("측정 시각이 기준점보다 앞선 좌표는 표시하지 않는다")
    void point_measured_before_anchor_is_not_displayable() {
        Gps stale = LocationFixture.createTrackPoint(
                LATITUDE, TRACK_ACCURACY, TRACK_START.minusSeconds(1), THIRTY_SECONDS_LATER);

        boolean displayable = stale.isDisplayableAfter(ANCHOR);

        assertThat(displayable).isFalse();
    }

    @ParameterizedTest(name = "위도 차 {0}도(≈{1}m) / 수신 간격 30초 → 표시 {2}")
    @CsvSource({
            "0.01340, 1490, true",
            "0.01358, 1510, false"
    })
    @DisplayName("측정 시각이 없으면 서버 수신 시각으로 속도를 잰다")
    void falls_back_to_received_time_without_measured_time(double latitudeDelta, int approxMeters, boolean expected) {
        Gps anchor = LocationFixture.createTrackPoint(LATITUDE, null, null, TRACK_START);
        Gps moved = LocationFixture.createTrackPoint(LATITUDE + latitudeDelta, null, null, THIRTY_SECONDS_LATER);

        boolean displayable = moved.isDisplayableAfter(anchor);

        assertThat(displayable).isEqualTo(expected);
    }

    @Test
    @DisplayName("1km 튀었다가 30초 뒤 원위치로 돌아온 좌표는 튐이다")
    void point_jumping_away_and_back_is_spike() {
        Gps jumped = LocationFixture.createTrackPoint(
                LATITUDE + SPIKE_1KM_LATITUDE_DELTA, TRACK_ACCURACY, THIRTY_SECONDS_LATER);
        Gps returned = LocationFixture.createTrackPoint(LATITUDE, TRACK_ACCURACY, ONE_MINUTE_LATER);

        boolean spike = jumped.isSpikeBetween(ANCHOR, returned);

        assertThat(spike).isTrue();
    }

    @ParameterizedTest(name = "위도 차 {0}도(≈{1}m) 튀었다 원위치 → 튐 {2}")
    @CsvSource({
            "0.0017896, 199, false",
            "0.0018076, 201, true"
    })
    @DisplayName("앞뒤 좌표에서 200m 이상 떨어져야 튐이다")
    void spike_requires_200m_from_both_neighbors(double latitudeDelta, int approxMeters, boolean expected) {
        Gps jumped = LocationFixture.createTrackPoint(LATITUDE + latitudeDelta, TRACK_ACCURACY, THIRTY_SECONDS_LATER);
        Gps returned = LocationFixture.createTrackPoint(LATITUDE, TRACK_ACCURACY, ONE_MINUTE_LATER);

        boolean spike = jumped.isSpikeBetween(ANCHOR, returned);

        assertThat(spike).isEqualTo(expected);
    }

    @ParameterizedTest(name = "260m 튄 좌표에서 뒤 좌표까지 ≈{1}m → 튐 {2}")
    @CsvSource({
            "0.0005486, 199, false",
            "0.0005306, 201, true"
    })
    @DisplayName("뒤 좌표와의 거리만 200m 미만이어도 튐이 아니다")
    void spike_requires_200m_from_next_neighbor(double returnedLatitudeDelta, int approxInboundMeters, boolean expected) {
        Gps jumped = LocationFixture.createTrackPoint(LATITUDE + JUMP_260M_LATITUDE_DELTA, TRACK_ACCURACY, THIRTY_SECONDS_LATER);
        Gps returned = LocationFixture.createTrackPoint(
                LATITUDE + returnedLatitudeDelta, TRACK_ACCURACY, ONE_MINUTE_LATER);

        boolean spike = jumped.isSpikeBetween(ANCHOR, returned);

        assertThat(spike).isEqualTo(expected);
    }

    @ParameterizedTest(name = "앞뒤 좌표 간격 ≈{1}m → 튐 {2}")
    @CsvSource({
            "0.0008903, 99, true",
            "0.0009083, 101, false"
    })
    @DisplayName("뒤 좌표가 앞 좌표의 100m 이내로 돌아와야 튐이다")
    void spike_requires_return_within_100m(double returnedLatitudeDelta, int approxReturnGapMeters, boolean expected) {
        Gps jumped = LocationFixture.createTrackPoint(
                LATITUDE + SPIKE_1KM_LATITUDE_DELTA, TRACK_ACCURACY, THIRTY_SECONDS_LATER);
        Gps returned = LocationFixture.createTrackPoint(
                LATITUDE + returnedLatitudeDelta, TRACK_ACCURACY, ONE_MINUTE_LATER);

        boolean spike = jumped.isSpikeBetween(ANCHOR, returned);

        assertThat(spike).isEqualTo(expected);
    }

    @ParameterizedTest(name = "300m 왕복에 {0}초 → 튐 {1}")
    @CsvSource({
            "210, false",
            "190, true"
    })
    @DisplayName("300m를 다녀오는 데 걸린 시간으로 낸 왕복 속도가 3m/s를 넘어야 튐이다")
    void spike_requires_round_trip_faster_than_3mps(long roundTripSeconds, boolean expected) {
        Gps visited = LocationFixture.createTrackPoint(
                LATITUDE + VISIT_300M_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(roundTripSeconds / 2));
        Gps returned = LocationFixture.createTrackPoint(
                LATITUDE, TRACK_ACCURACY, TRACK_START.plusSeconds(roundTripSeconds));

        boolean spike = visited.isSpikeBetween(ANCHOR, returned);

        assertThat(spike).isEqualTo(expected);
    }

    @Test
    @DisplayName("뒤 좌표의 측정 시각이 앞 좌표와 같으면 튐으로 보지 않는다")
    void spike_is_not_judged_without_elapsed_time() {
        Gps jumped = LocationFixture.createTrackPoint(
                LATITUDE + SPIKE_1KM_LATITUDE_DELTA, TRACK_ACCURACY, THIRTY_SECONDS_LATER);
        Gps returned = LocationFixture.createTrackPoint(LATITUDE, TRACK_ACCURACY, TRACK_START);

        boolean spike = jumped.isSpikeBetween(ANCHOR, returned);

        assertThat(spike).isFalse();
    }
}
