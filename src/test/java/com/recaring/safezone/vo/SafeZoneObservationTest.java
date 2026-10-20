package com.recaring.safezone.vo;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static com.recaring.location.fixture.LocationFixture.LATITUDE;
import static com.recaring.location.fixture.LocationFixture.LONGITUDE;
import static com.recaring.location.fixture.LocationFixture.TRACK_ACCURACY;
import static com.recaring.location.fixture.LocationFixture.TRACK_START;
import static com.recaring.location.fixture.LocationFixture.ZONE_RADIUS_300M;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("안심존 진입·이탈 히스테리시스 단위 테스트")
class SafeZoneObservationTest {

    private static final String HOME_KEY = LocationFixture.HOME_ZONE_KEY;
    private static final String HOSPITAL_KEY = LocationFixture.HOSPITAL_ZONE_KEY;
    private static final SafeZoneInfo HOME_300M =
            SafeZoneFixture.createSafeZoneInfoAt(HOME_KEY, LATITUDE, LONGITUDE, ZONE_RADIUS_300M);

    @ParameterizedTest(name = "중심에서 ≈{1}m → 안 유지 {2}")
    @CsvSource({
            "0.0029588, 329, true",
            "0.0029768, 331, false"
    })
    @DisplayName("존 안이던 대상자는 반경에 여유 폭 30m를 더한 거리를 넘어야 밖이 된다")
    void inside_ward_exits_beyond_radius_plus_margin(double latitudeDelta, int approxMeters, boolean expectedInside) {
        SafeZoneObservation observation = observe(List.of(HOME_300M), latitudeDelta, TRACK_ACCURACY);

        Set<String> current = observation.resolve(Set.of(HOME_KEY), Set.of(HOME_KEY));

        assertThat(current.contains(HOME_KEY)).isEqualTo(expectedInside);
    }

    @ParameterizedTest(name = "중심에서 ≈{1}m → 안 {2}")
    @CsvSource({
            "0.0024372, 271, false",
            "0.0024192, 269, true"
    })
    @DisplayName("존 밖이던 대상자는 반경에서 여유 폭 30m를 뺀 거리 안으로 들어와야 안이 된다")
    void outside_ward_enters_within_radius_minus_margin(double latitudeDelta, int approxMeters, boolean expectedInside) {
        SafeZoneObservation observation = observe(List.of(HOME_300M), latitudeDelta, TRACK_ACCURACY);

        Set<String> current = observation.resolve(Set.of(), Set.of(HOME_KEY));

        assertThat(current.contains(HOME_KEY)).isEqualTo(expectedInside);
    }

    @ParameterizedTest(name = "accuracy 80m, 중심에서 ≈{1}m → 안 유지 {2}")
    @CsvSource({
            "0.0034084, 379, true",
            "0.0034264, 381, false"
    })
    @DisplayName("accuracy가 30m보다 크면 그 값이 여유 폭이 된다")
    void accuracy_larger_than_minimum_widens_margin(double latitudeDelta, int approxMeters, boolean expectedInside) {
        SafeZoneObservation observation = observe(List.of(HOME_300M), latitudeDelta, 80.0);

        Set<String> current = observation.resolve(Set.of(HOME_KEY), Set.of(HOME_KEY));

        assertThat(current.contains(HOME_KEY)).isEqualTo(expectedInside);
    }

    @ParameterizedTest(name = "반경 100m, accuracy 100m, 중심에서 ≈{1}m → 안 {2}")
    @CsvSource({
            "0.0004407, 49, true",
            "0.0004587, 51, false"
    })
    @DisplayName("여유 폭이 반경보다 커도 반경의 절반 안으로 들어오면 진입한다")
    void entry_radius_never_shrinks_below_half_radius(double latitudeDelta, int approxMeters, boolean expectedInside) {
        SafeZoneInfo small = SafeZoneFixture.createSafeZoneInfoAt(HOME_KEY, LATITUDE, LONGITUDE, 100);
        SafeZoneObservation observation = observe(List.of(small), latitudeDelta, 100.0);

        Set<String> current = observation.resolve(Set.of(), Set.of(HOME_KEY));

        assertThat(current.contains(HOME_KEY)).isEqualTo(expectedInside);
    }

    @Test
    @DisplayName("직전에 속해 있던 존이 삭제되면 현재 상태에서 빠진다")
    void deleted_zone_is_dropped_from_current_state() {
        SafeZoneObservation observation = observe(List.of(HOME_300M), 0.0, TRACK_ACCURACY);

        Set<String> current = observation.resolve(Set.of(HOME_KEY, HOSPITAL_KEY), Set.of(HOME_KEY));

        assertThat(current).containsExactly(HOME_KEY);
    }

    private SafeZoneObservation observe(List<SafeZoneInfo> zones, double latitudeDelta, Double accuracy) {
        Gps gps = LocationFixture.createTrackPoint(LATITUDE + latitudeDelta, accuracy, TRACK_START);
        return SafeZoneObservation.of(zones, gps);
    }
}
