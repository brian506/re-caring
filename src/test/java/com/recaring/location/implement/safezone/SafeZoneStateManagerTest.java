package com.recaring.location.implement.safezone;

import com.recaring.location.dataaccess.entity.SafeZoneState;
import com.recaring.location.dataaccess.repository.SafeZoneStateRepository;
import com.recaring.location.fixture.LocationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneStateManager 단위 테스트")
class SafeZoneStateManagerTest {

    private static final String HOME_KEY = "safe-zone-home";
    private static final String HOSPITAL_KEY = "safe-zone-hospital";

    @InjectMocks
    private SafeZoneStateManager safeZoneStateManager;

    @Mock
    private SafeZoneStateRepository safeZoneStateRepository;

    @Captor
    private ArgumentCaptor<SafeZoneState> stateCaptor;

    @Test
    @DisplayName("저장된 상태가 없으면 기준선만 세우고 비교 대상 없음을 반환한다")
    void replaceAndGetPrevious_returns_empty_on_first_observation() {
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.empty());

        Optional<Set<String>> result = safeZoneStateManager
                .replaceAndGetPrevious(LocationFixture.WARD_KEY, Set.of(HOME_KEY));

        assertThat(result).isEmpty();
        then(safeZoneStateRepository).should(times(1)).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getWardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(stateCaptor.getValue().getSafeZoneKeys()).isEqualTo(HOME_KEY);
    }

    @Test
    @DisplayName("저장된 상태가 있으면 직전 값을 반환하고 현재 값으로 교체한다")
    void replaceAndGetPrevious_returns_previous_and_replaces() {
        SafeZoneState stored = SafeZoneState.builder()
                .wardMemberKey(LocationFixture.WARD_KEY)
                .safeZoneKeys(HOME_KEY)
                .build();
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<Set<String>> result = safeZoneStateManager
                .replaceAndGetPrevious(LocationFixture.WARD_KEY, Set.of(HOSPITAL_KEY));

        assertThat(result).contains(Set.of(HOME_KEY));
        assertThat(stored.getSafeZoneKeys()).isEqualTo(HOSPITAL_KEY);
        then(safeZoneStateRepository).should(never()).save(any(SafeZoneState.class));
    }

    @Test
    @DisplayName("존 밖이었던 상태는 빈 집합으로 복원한다 — 최초 관측과 구분된다")
    void replaceAndGetPrevious_restores_outside_state_as_empty_set() {
        SafeZoneState stored = SafeZoneState.builder()
                .wardMemberKey(LocationFixture.WARD_KEY)
                .safeZoneKeys("")
                .build();
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<Set<String>> result = safeZoneStateManager
                .replaceAndGetPrevious(LocationFixture.WARD_KEY, Set.of(HOME_KEY));

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("여러 존에 속해 있던 상태를 모두 복원한다")
    void replaceAndGetPrevious_restores_multiple_keys() {
        SafeZoneState stored = SafeZoneState.builder()
                .wardMemberKey(LocationFixture.WARD_KEY)
                .safeZoneKeys(HOME_KEY + "," + HOSPITAL_KEY)
                .build();
        given(safeZoneStateRepository.findByWardMemberKey(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(stored));

        Optional<Set<String>> result = safeZoneStateManager
                .replaceAndGetPrevious(LocationFixture.WARD_KEY, Set.of());

        assertThat(result).contains(Set.of(HOME_KEY, HOSPITAL_KEY));
        assertThat(stored.getSafeZoneKeys()).isEmpty();
    }

    @Test
    @DisplayName("상태 삭제는 리포지토리에 위임한다")
    void delete_delegates_to_repository() {
        safeZoneStateManager.delete(LocationFixture.WARD_KEY);

        then(safeZoneStateRepository).should(times(1)).deleteByWardMemberKey(LocationFixture.WARD_KEY);
    }
}
