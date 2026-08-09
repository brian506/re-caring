package com.recaring.location.vo;

import com.recaring.location.fixture.LocationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gps 단위 테스트")
class GpsTest {

    @Test
    @DisplayName("오차가 임계값 이하면 판정에 쓸 수 있다")
    void isAccurate_within_threshold() {
        assertThat(LocationFixture.createGpsWithAccuracy(15.0).isAccurate()).isTrue();
    }

    @Test
    @DisplayName("오차가 임계값과 같으면 판정에 쓸 수 있다")
    void isAccurate_on_threshold() {
        assertThat(LocationFixture.createGpsWithAccuracy(100.0).isAccurate()).isTrue();
    }

    @Test
    @DisplayName("오차가 임계값을 넘으면 판정에 쓸 수 없다")
    void isAccurate_over_threshold() {
        assertThat(LocationFixture.createGpsWithAccuracy(100.1).isAccurate()).isFalse();
    }

    @Test
    @DisplayName("기기가 정확도를 보고하지 않으면 판정을 막지 않는다")
    void isAccurate_when_accuracy_absent() {
        assertThat(LocationFixture.createGpsWithAccuracy(null).isAccurate()).isTrue();
    }

    @Test
    @DisplayName("기기 측정 시각이 있으면 그 값을, 없으면 서버 수신 시각을 쓴다")
    void occurredAt_prefers_measured_at() {
        Gps measured = LocationFixture.createGps();
        Gps unmeasured = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE,
                LocationFixture.RECORDED_AT, LocationFixture.ACCURACY, LocationFixture.BATTERY,
                LocationFixture.SPEED, null);

        assertThat(measured.occurredAt()).isEqualTo(LocationFixture.MEASURED_AT);
        assertThat(unmeasured.occurredAt()).isEqualTo(LocationFixture.RECORDED_AT);
    }
}
