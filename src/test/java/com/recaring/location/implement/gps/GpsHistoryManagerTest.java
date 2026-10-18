package com.recaring.location.implement.gps;

import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.recaring.location.fixture.LocationFixture.INACCURATE_ACCURACY;
import static com.recaring.location.fixture.LocationFixture.JUMP_15KM_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.LATITUDE;
import static com.recaring.location.fixture.LocationFixture.SPIKE_1KM_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.TRACK_ACCURACY;
import static com.recaring.location.fixture.LocationFixture.TRACK_START;
import static com.recaring.location.fixture.LocationFixture.WALK_50M_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.WARD_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsHistoryManager 단위 테스트")
class GpsHistoryManagerTest {

    private static final LocalDate TRACK_DATE = TRACK_START.toLocalDate();
    private static final int RECENT_LOOKBACK = 20;

    @InjectMocks
    private GpsHistoryManager gpsHistoryManager;

    @Mock
    private GpsHistoryRepository gpsHistoryRepository;

    @Test
    @DisplayName("하루 경로에서 튀었다 돌아온 좌표만 빠지고 나머지는 시간순으로 유지된다")
    void daily_history_drops_spike_and_keeps_order() {
        given(gpsHistoryRepository.findDailyGpsHistory(WARD_KEY, TRACK_DATE)).willReturn(List.of(
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, TRACK_ACCURACY, TRACK_START),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + JUMP_15KM_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(30)),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + WALK_50M_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(60))));

        List<Gps> history = gpsHistoryManager.findDailyGpsHistory(WARD_KEY, TRACK_DATE);

        assertThat(history).extracting(Gps::latitude)
                .containsExactly(LATITUDE, LATITUDE + WALK_50M_LATITUDE_DELTA);
    }

    @Test
    @DisplayName("튀어서 제외된 좌표는 다음 좌표의 기준점이 되지 않는다")
    void dropped_spike_does_not_become_anchor() {
        given(gpsHistoryRepository.findDailyGpsHistory(WARD_KEY, TRACK_DATE)).willReturn(List.of(
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, TRACK_ACCURACY, TRACK_START),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + JUMP_15KM_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(30)),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + JUMP_15KM_LATITUDE_DELTA + WALK_50M_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(60))));

        List<Gps> history = gpsHistoryManager.findDailyGpsHistory(WARD_KEY, TRACK_DATE);

        assertThat(history).extracting(Gps::latitude).containsExactly(LATITUDE);
    }

    @Test
    @DisplayName("속도 기준으로는 갈 수 있는 1km라도 혼자 튀었다 돌아온 좌표는 경로에서 빠진다")
    void daily_history_drops_point_that_jumps_away_and_back() {
        given(gpsHistoryRepository.findDailyGpsHistory(WARD_KEY, TRACK_DATE)).willReturn(List.of(
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, TRACK_ACCURACY, TRACK_START),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + SPIKE_1KM_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(30)),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + WALK_50M_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(60))));

        List<Gps> history = gpsHistoryManager.findDailyGpsHistory(WARD_KEY, TRACK_DATE);

        assertThat(history).extracting(Gps::latitude)
                .containsExactly(LATITUDE, LATITUDE + WALK_50M_LATITUDE_DELTA);
    }

    @Test
    @DisplayName("하루의 마지막 좌표는 비교할 다음 좌표가 없어 튀었더라도 남는다")
    void daily_history_keeps_last_point_without_next() {
        given(gpsHistoryRepository.findDailyGpsHistory(WARD_KEY, TRACK_DATE)).willReturn(List.of(
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, TRACK_ACCURACY, TRACK_START),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + WALK_50M_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(30)),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + WALK_50M_LATITUDE_DELTA + SPIKE_1KM_LATITUDE_DELTA, TRACK_ACCURACY, TRACK_START.plusSeconds(60))));

        List<Gps> history = gpsHistoryManager.findDailyGpsHistory(WARD_KEY, TRACK_DATE);

        assertThat(history).extracting(Gps::latitude).containsExactly(
                LATITUDE, LATITUDE + WALK_50M_LATITUDE_DELTA, LATITUDE + WALK_50M_LATITUDE_DELTA + SPIKE_1KM_LATITUDE_DELTA);
    }

    @Test
    @DisplayName("정확도가 100m를 넘는 좌표는 경로에서 빠진다")
    void daily_history_drops_inaccurate_point() {
        given(gpsHistoryRepository.findDailyGpsHistory(WARD_KEY, TRACK_DATE)).willReturn(List.of(
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, TRACK_ACCURACY, TRACK_START),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + WALK_50M_LATITUDE_DELTA, INACCURATE_ACCURACY, TRACK_START.plusSeconds(30)),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + WALK_50M_LATITUDE_DELTA * 2, TRACK_ACCURACY, TRACK_START.plusSeconds(60))));

        List<Gps> history = gpsHistoryManager.findDailyGpsHistory(WARD_KEY, TRACK_DATE);

        assertThat(history).extracting(Gps::accuracy).containsExactly(TRACK_ACCURACY, TRACK_ACCURACY);
    }

    @Test
    @DisplayName("가장 최근 좌표가 부정확하면 그 이전의 정확한 좌표를 마지막 위치로 돌려준다")
    void latest_displayable_skips_inaccurate_newest_point() {
        given(gpsHistoryRepository.findRecent(WARD_KEY, RECENT_LOOKBACK)).willReturn(List.of(
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE + WALK_50M_LATITUDE_DELTA, INACCURATE_ACCURACY, TRACK_START.plusSeconds(30)),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, TRACK_ACCURACY, TRACK_START)));

        Optional<Gps> latest = gpsHistoryManager.findLatestDisplayable(WARD_KEY);

        assertThat(latest).map(Gps::measuredAt).contains(TRACK_START);
    }

    @Test
    @DisplayName("최근 좌표가 모두 부정확하면 마지막 위치가 없다")
    void latest_displayable_is_empty_when_all_recent_points_are_inaccurate() {
        given(gpsHistoryRepository.findRecent(WARD_KEY, RECENT_LOOKBACK)).willReturn(List.of(
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, INACCURATE_ACCURACY, TRACK_START.plusSeconds(30)),
                LocationFixture.createGpsHistory(WARD_KEY, LATITUDE, INACCURATE_ACCURACY, TRACK_START)));

        Optional<Gps> latest = gpsHistoryManager.findLatestDisplayable(WARD_KEY);

        assertThat(latest).isEmpty();
    }
}
