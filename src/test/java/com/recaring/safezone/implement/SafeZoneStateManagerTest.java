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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static com.recaring.safezone.fixture.SafeZoneFixture.ZONE_EDITED_AT;
import static com.recaring.safezone.fixture.SafeZoneFixture.ZONE_UPDATED_AT;
import static com.recaring.safezone.fixture.SafeZoneFixture.zoneVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
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
    private static final SafeZoneInfo EDITED_HOME_300M =
            SafeZoneFixture.createSafeZoneInfoAt(HOME_KEY, LATITUDE, LONGITUDE, ZONE_RADIUS_300M, ZONE_EDITED_AT);

    private static final String HOME_VERSION = zoneVersion(HOME_KEY, ZONE_UPDATED_AT);
    private static final String HOSPITAL_VERSION = zoneVersion(HOSPITAL_KEY, ZONE_UPDATED_AT);

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
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(HOME_KEY, HOME_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(AT_320M_LATITUDE_DELTA, HOME_300M));

        assertThat(result).isPresent();
        assertThat(result.get().enteredKeys()).isEmpty();
        assertThat(result.get().exitedKeys()).isEmpty();
        assertThat(stored.getSafeZoneKeys()).isEqualTo(HOME_KEY);
    }

    @Test
    @DisplayName("존 안이던 대상자가 여유 폭 바깥으로 나가면 이탈하고 밖 상태로 저장한다")
    void clear_exit_returns_exited_and_stores_outside() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(HOME_KEY, HOME_VERSION);
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
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState("", HOME_VERSION);
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
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(
                HOME_KEY + "," + HOSPITAL_KEY, HOME_VERSION + "," + HOSPITAL_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(SPIKE_1KM_LATITUDE_DELTA, HOME_300M, HOSPITAL_300M));

        assertThat(result).map(SafeZoneTransition::exitedKeys).contains(Set.of(HOME_KEY, HOSPITAL_KEY));
        assertThat(stored.getSafeZoneKeys()).isEmpty();
    }

    @Test
    @DisplayName("안에 있던 존이 수정된 뒤 위치가 반경 밖이면 이탈 없이 밖 상태로 기준선을 다시 세운다")
    void edited_zone_no_longer_covering_ward_rebaselines_without_exit() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(HOME_KEY, HOME_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(AT_340M_LATITUDE_DELTA, EDITED_HOME_300M));

        assertThat(result).isPresent();
        assertThat(result.get().enteredKeys()).isEmpty();
        assertThat(result.get().exitedKeys()).isEmpty();
        assertThat(stored.getSafeZoneKeys()).isEmpty();
    }

    @Test
    @DisplayName("밖에 있던 존이 수정돼 현재 위치를 포함하게 되면 진입 없이 안 상태로 기준선을 다시 세운다")
    void edited_zone_now_covering_ward_rebaselines_without_entry() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState("", HOME_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(0.0, EDITED_HOME_300M));

        assertThat(result).isPresent();
        assertThat(result.get().enteredKeys()).isEmpty();
        assertThat(result.get().exitedKeys()).isEmpty();
        assertThat(stored.getSafeZoneKeys()).isEqualTo(HOME_KEY);
    }

    @Test
    @DisplayName("새로 등록된 존 안에 있으면 진입 없이 안 상태로 저장된다")
    void newly_registered_zone_is_stored_as_inside_without_entry() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(HOME_KEY, HOME_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(0.0, HOME_300M, HOSPITAL_300M));

        assertThat(result).isPresent();
        assertThat(result.get().enteredKeys()).isEmpty();
        assertThat(result.get().exitedKeys()).isEmpty();
        assertThat(stored.getSafeZoneKeys()).isEqualTo(HOME_KEY + "," + HOSPITAL_KEY);
    }

    @Test
    @DisplayName("한 존이 수정돼도 버전이 같은 다른 존을 벗어나면 이탈로 판정한다")
    void unchanged_zone_keeps_detecting_exit_while_other_zone_is_edited() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(
                HOME_KEY + "," + HOSPITAL_KEY, HOME_VERSION + "," + HOSPITAL_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY,
                        observeAt(SPIKE_1KM_LATITUDE_DELTA, EDITED_HOME_300M, HOSPITAL_300M));

        assertThat(result).map(SafeZoneTransition::exitedKeys).contains(Set.of(HOSPITAL_KEY));
        assertThat(result).map(SafeZoneTransition::enteredKeys).contains(Set.of());
        assertThat(stored.getSafeZoneKeys()).isEmpty();
    }

    @Test
    @DisplayName("기준선을 세운 뒤 다음 관측에서 같은 버전의 존을 벗어나면 이탈로 판정한다")
    void baseline_versions_are_recognized_on_next_observation() {
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.empty());
        safeZoneStateManager.transition(LocationFixture.WARD_KEY, observeAt(0.0, HOME_300M));
        then(safeZoneStateRepository).should().save(stateCaptor.capture());
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stateCaptor.getValue()));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(AT_340M_LATITUDE_DELTA, HOME_300M));

        assertThat(result).map(SafeZoneTransition::exitedKeys).contains(Set.of(HOME_KEY));
    }

    @Test
    @DisplayName("버전 정보가 없는 배포 전 상태는 진입·이탈 없이 기준선만 다시 세운다")
    void legacy_state_without_versions_rebaselines_without_transition() {
        SafeZoneState stored = SafeZoneFixture.createLegacySafeZoneState(HOME_KEY);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(AT_340M_LATITUDE_DELTA, HOME_300M));

        assertThat(result).isPresent();
        assertThat(result.get().enteredKeys()).isEmpty();
        assertThat(result.get().exitedKeys()).isEmpty();
        assertThat(stored.getSafeZoneKeys()).isEmpty();
    }

    @ParameterizedTest(name = "중심에서 ≈{1}m → 저장된 존 \"{2}\"")
    @CsvSource({
            "0.002608, 290, safe-zone-home",
            "0.0028778, 320, ''"
    })
    @DisplayName("수정된 존의 기준선은 여유 폭 없이 반경 300m만으로 판정한다")
    void rebaseline_uses_radius_without_margin(double latitudeDelta, int approxMeters, String expectedKeys) {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState("", HOME_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(latitudeDelta, EDITED_HOME_300M));

        assertThat(result).map(SafeZoneTransition::enteredKeys).contains(Set.of());
        assertThat(stored.getSafeZoneKeys()).isEqualTo(expectedKeys);
    }

    @Test
    @DisplayName("직전에 속해 있던 존이 삭제되면 이탈로 판정하지 않는다")
    void deleted_zone_is_not_reported_as_exit() {
        SafeZoneState stored = SafeZoneFixture.createSafeZoneState(
                HOME_KEY + "," + HOSPITAL_KEY, HOME_VERSION + "," + HOSPITAL_VERSION);
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<SafeZoneTransition> result = safeZoneStateManager
                .transition(LocationFixture.WARD_KEY, observeAt(0.0, HOME_300M));

        assertThat(result).map(SafeZoneTransition::exitedKeys).contains(Set.of());
        assertThat(stored.getSafeZoneKeys()).isEqualTo(HOME_KEY);
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
