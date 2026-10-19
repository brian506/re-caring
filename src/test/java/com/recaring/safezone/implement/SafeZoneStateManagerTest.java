package com.recaring.safezone.implement;

import com.recaring.safezone.dataaccess.entity.SafeZoneState;
import com.recaring.safezone.dataaccess.repository.SafeZoneStateRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import com.recaring.safezone.vo.SafeZoneObservation;
import com.recaring.safezone.vo.SafeZoneTransition;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.vo.SafeZoneInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.recaring.location.fixture.LocationFixture.LATITUDE;
import static com.recaring.location.fixture.LocationFixture.LONGITUDE;
import static com.recaring.location.fixture.LocationFixture.SPIKE_1KM_LATITUDE_DELTA;
import static com.recaring.location.fixture.LocationFixture.TRACK_ACCURACY;
import static com.recaring.location.fixture.LocationFixture.TRACK_START;
import static com.recaring.location.fixture.LocationFixture.ZONE_RADIUS_300M;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneStateManager 단위 테스트")
class SafeZoneStateManagerTest {

    private static final String HOME_KEY = LocationFixture.HOME_ZONE_KEY;
    private static final String HOSPITAL_KEY = LocationFixture.HOSPITAL_ZONE_KEY;
    private static final SafeZoneInfo HOME_300M =
            SafeZoneFixture.createSafeZoneInfoAt(HOME_KEY, LATITUDE, LONGITUDE, ZONE_RADIUS_300M);
    private static final SafeZoneInfo HOSPITAL_300M =
            SafeZoneFixture.createSafeZoneInfoAt(HOSPITAL_KEY, LATITUDE, LONGITUDE, ZONE_RADIUS_300M);

    private static final double AT_290M_LATITUDE_DELTA = 0.002608;
    private static final double AT_320M_LATITUDE_DELTA = 0.0028778;
    private static final double AT_340M_LATITUDE_DELTA = 0.0030577;

    @InjectMocks
    private SafeZoneStateManager safeZoneStateManager;

    @Mock
    private SafeZoneStateRepository safeZoneStateRepository;

    @Captor
    private ArgumentCaptor<SafeZoneState> stateCaptor;

    @Test
    @DisplayName("저장된 상태가 없으면 여유 폭 없이 반경 기준으로 기준선을 세우고 전이 없음을 반환한다")
    void first_observation_sets_baseline_by_radius() {
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(AT_290M_LATITUDE_DELTA, HOME_300M));

        assertThat(result).isEmpty();
        then(safeZoneStateRepository).should(times(1)).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getWardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(stateCaptor.getValue().getSafeZoneKeys()).isEqualTo(HOME_KEY);
    }

    @Test
    @DisplayName("존 안이던 대상자가 경계 밖으로 조금 흔들리면 안으로 유지되고 진입·이탈이 없다")
    void small_jitter_across_boundary_keeps_previous_state() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(HOME_KEY);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(AT_320M_LATITUDE_DELTA, HOME_300M));

        assertThat(result).isPresent();
        assertThat(result.get().enteredKeys()).isEmpty();
        assertThat(result.get().exitedKeys()).isEmpty();
        assertThat(stored.getSafeZoneKeys()).isEqualTo(HOME_KEY);
        then(safeZoneStateRepository).should(never()).save(any(SafeZoneState.class));
    }

    @Test
    @DisplayName("존 안이던 대상자가 여유 폭 바깥으로 나가면 이탈하고 밖 상태로 저장한다")
    void clear_exit_returns_exited_and_stores_outside() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(HOME_KEY);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(AT_340M_LATITUDE_DELTA, HOME_300M));

        assertThat(result).map(SafeZoneTransition::exitedKeys).contains(Set.of(HOME_KEY));
        assertThat(stored.getSafeZoneKeys()).isEmpty();
    }

    @Test
    @DisplayName("존 밖이었던 상태는 빈 집합으로 복원되어 중심에 들어오면 진입으로 판정한다")
    void outside_state_is_restored_as_empty_set() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState("");
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(0.0, HOME_300M));

        assertThat(result).map(SafeZoneTransition::enteredKeys).contains(Set.of(HOME_KEY));
        assertThat(stored.getSafeZoneKeys()).isEqualTo(HOME_KEY);
    }

    @Test
    @DisplayName("여러 존에 속해 있던 상태를 모두 복원해 함께 벗어나면 모두 이탈로 판정한다")
    void multiple_stored_keys_are_all_restored() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(HOME_KEY + "," + HOSPITAL_KEY);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(SPIKE_1KM_LATITUDE_DELTA, HOME_300M, HOSPITAL_300M));

        assertThat(result).map(SafeZoneTransition::exitedKeys).contains(Set.of(HOME_KEY, HOSPITAL_KEY));
        assertThat(stored.getSafeZoneKeys()).isEmpty();
    }

    @Test
    @DisplayName("상태 삭제는 리포지토리에 위임한다")
    void delete_delegates_to_repository() {
        safeZoneStateManager.delete(LocationFixture.WARD_KEY);

        then(safeZoneStateRepository).should(times(1)).deleteByWardMemberKey(LocationFixture.WARD_KEY);
    }

    private SafeZoneObservation observeAt(double latitudeDelta, SafeZoneInfo... zones) {
        Gps gps = LocationFixture.createTrackPoint(LATITUDE + latitudeDelta, TRACK_ACCURACY, TRACK_START);
        return SafeZoneObservation.of(List.of(zones), gps);
    }
}
