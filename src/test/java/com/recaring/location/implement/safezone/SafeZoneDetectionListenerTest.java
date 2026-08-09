package com.recaring.location.implement.safezone;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.event.SafeZoneEnteredEvent;
import com.recaring.location.event.SafeZoneExitedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.implement.SafeZoneReader;
import com.recaring.safezone.vo.SafeZoneInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneDetectionListener 단위 테스트")
class SafeZoneDetectionListenerTest {

    private static final String HOME_KEY = "safe-zone-home";
    private static final String HOME_NAME = "집";
    private static final String HOSPITAL_KEY = "safe-zone-hospital";
    private static final String HOSPITAL_NAME = "병원";

    // GPS 좌표에서 충분히 멀어 어떤 반경으로도 포함되지 않는 오프셋.
    private static final double FAR_OFFSET = 1.0;

    @InjectMocks
    private SafeZoneDetectionListener safeZoneDetectionListener;

    @Mock
    private SafeZoneReader safeZoneReader;

    @Mock
    private SafeZoneStateManager safeZoneStateManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @Test
    @DisplayName("오차가 큰 좌표는 존 조회도 상태 갱신도 하지 않는다")
    void onGpsSaved_skips_when_accuracy_unreliable() {
        Gps inaccurate = LocationFixture.createGpsWithAccuracy(150.0);

        safeZoneDetectionListener.onGpsSaved(new GpsSavedEvent(LocationFixture.WARD_KEY, inaccurate));

        then(safeZoneReader).shouldHaveNoInteractions();
        then(safeZoneStateManager).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("안심존이 하나도 없으면 상태만 지우고 판정하지 않는다")
    void onGpsSaved_deletes_state_when_no_zone() {
        given(safeZoneReader.findAllByWardMemberKey(LocationFixture.WARD_KEY)).willReturn(List.of());

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(safeZoneStateManager).should(times(1)).delete(LocationFixture.WARD_KEY);
        then(safeZoneStateManager).should(never()).replaceAndGetPrevious(anyString(), any());
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("최초 관측은 기준선만 세우고 알리지 않는다")
    void onGpsSaved_does_not_publish_on_first_observation() {
        givenZones(nearZone(HOME_KEY, HOME_NAME));
        given(safeZoneStateManager.replaceAndGetPrevious(LocationFixture.WARD_KEY, Set.of(HOME_KEY)))
                .willReturn(Optional.empty());

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존 밖에 있다가 들어오면 진입 이벤트를 발행한다")
    void onGpsSaved_publishes_entered() {
        givenZones(nearZone(HOME_KEY, HOME_NAME));
        givenPreviousKeys(Set.of(HOME_KEY), Set.of());

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        SafeZoneEnteredEvent published = (SafeZoneEnteredEvent) eventCaptor.getValue();
        assertThat(published.wardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(published.safeZoneKey()).isEqualTo(HOME_KEY);
        assertThat(published.safeZoneName()).isEqualTo(HOME_NAME);
        assertThat(published.detectedAt()).isEqualTo(LocationFixture.MEASURED_AT);
    }

    @Test
    @DisplayName("존 안에 있다가 나가면 이탈 이벤트를 발행한다")
    void onGpsSaved_publishes_exited() {
        givenZones(farZone(HOME_KEY, HOME_NAME));
        givenPreviousKeys(Set.of(), Set.of(HOME_KEY));

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        SafeZoneExitedEvent published = (SafeZoneExitedEvent) eventCaptor.getValue();
        assertThat(published.safeZoneKey()).isEqualTo(HOME_KEY);
        assertThat(published.safeZoneName()).isEqualTo(HOME_NAME);
    }

    @Test
    @DisplayName("존 안에 계속 머무르면 아무 이벤트도 발행하지 않는다")
    void onGpsSaved_publishes_nothing_while_staying() {
        givenZones(nearZone(HOME_KEY, HOME_NAME));
        givenPreviousKeys(Set.of(HOME_KEY), Set.of(HOME_KEY));

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존 밖에 계속 있어도 아무 이벤트도 발행하지 않는다")
    void onGpsSaved_publishes_nothing_while_outside() {
        givenZones(farZone(HOME_KEY, HOME_NAME));
        givenPreviousKeys(Set.of(), Set.of());

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("겹친 두 존에 동시에 들어가면 존마다 진입 이벤트를 발행한다")
    void onGpsSaved_publishes_entered_per_overlapping_zone() {
        givenZones(nearZone(HOME_KEY, HOME_NAME), nearZone(HOSPITAL_KEY, HOSPITAL_NAME));
        givenPreviousKeys(Set.of(HOME_KEY, HOSPITAL_KEY), Set.of());

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).should(times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .allMatch(SafeZoneEnteredEvent.class::isInstance)
                .extracting(event -> ((SafeZoneEnteredEvent) event).safeZoneKey())
                .containsExactly(HOME_KEY, HOSPITAL_KEY);
    }

    @Test
    @DisplayName("겹친 구역에서 한 존만 벗어나면 그 존만 이탈하고 남은 존은 재진입 알림이 없다")
    void onGpsSaved_publishes_only_the_zone_left() {
        givenZones(nearZone(HOME_KEY, HOME_NAME), farZone(HOSPITAL_KEY, HOSPITAL_NAME));
        givenPreviousKeys(Set.of(HOME_KEY), Set.of(HOME_KEY, HOSPITAL_KEY));

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
        SafeZoneExitedEvent published = (SafeZoneExitedEvent) eventCaptor.getValue();
        assertThat(published.safeZoneKey()).isEqualTo(HOSPITAL_KEY);
    }

    @Test
    @DisplayName("삭제되어 사라진 존은 이탈로 보지 않는다")
    void onGpsSaved_does_not_publish_exit_for_deleted_zone() {
        givenZones(nearZone(HOME_KEY, HOME_NAME));
        givenPreviousKeys(Set.of(HOME_KEY), Set.of(HOME_KEY, HOSPITAL_KEY));

        safeZoneDetectionListener.onGpsSaved(gpsSavedEvent());

        then(eventPublisher).shouldHaveNoInteractions();
    }

    private void givenZones(SafeZoneInfo... zones) {
        given(safeZoneReader.findAllByWardMemberKey(LocationFixture.WARD_KEY)).willReturn(List.of(zones));
    }

    private void givenPreviousKeys(Set<String> expectedCurrentKeys, Set<String> previousKeys) {
        given(safeZoneStateManager.replaceAndGetPrevious(LocationFixture.WARD_KEY, expectedCurrentKeys))
                .willReturn(Optional.of(previousKeys));
    }

    private SafeZoneInfo nearZone(String safeZoneKey, String name) {
        return SafeZoneFixture.createSafeZoneInfoAt(
                safeZoneKey, name, LocationFixture.LATITUDE, LocationFixture.LONGITUDE);
    }

    private SafeZoneInfo farZone(String safeZoneKey, String name) {
        return SafeZoneFixture.createSafeZoneInfoAt(
                safeZoneKey, name, LocationFixture.LATITUDE + FAR_OFFSET, LocationFixture.LONGITUDE);
    }

    private GpsSavedEvent gpsSavedEvent() {
        return new GpsSavedEvent(LocationFixture.WARD_KEY, LocationFixture.createGps());
    }
}
