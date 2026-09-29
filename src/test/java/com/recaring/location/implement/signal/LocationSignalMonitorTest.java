package com.recaring.location.implement.signal;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.event.LocationSignalLostEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("위치 수집 중단 감지 단위 테스트")
class LocationSignalMonitorTest {

    @InjectMocks
    private LocationSignalMonitor locationSignalMonitor;

    @Mock
    private CareRelationshipReader careRelationshipReader;
    @Mock
    private GpsLatestCacheManager gpsLatestCacheManager;
    @Mock
    private LocationSignalAlertManager locationSignalAlertManager;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("처음 감지한 끊김이면 그 대상자의 마지막 위치를 담아 수집 중단 이벤트를 발행한다")
    void detectLoss_publishes_event_with_last_gps_for_new_outage() {
        Gps lastGps = givenWardSilentForAnHour();
        given(locationSignalAlertManager.claim(LocationFixture.WARD_KEY, lastGps.recordedAt())).willReturn(true);

        locationSignalMonitor.detectLoss();

        ArgumentCaptor<LocationSignalLostEvent> published = ArgumentCaptor.forClass(LocationSignalLostEvent.class);
        then(eventPublisher).should().publishEvent(published.capture());
        assertThat(published.getValue().wardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(published.getValue().lastGps()).isEqualTo(lastGps);
        then(locationSignalAlertManager).should(never()).release(anyString(), any());
    }

    @Test
    @DisplayName("같은 끊김 구간을 이미 알렸으면 다시 알리지 않는다")
    void detectLoss_skips_when_outage_already_claimed() {
        Gps lastGps = givenWardSilentForAnHour();
        given(locationSignalAlertManager.claim(LocationFixture.WARD_KEY, lastGps.recordedAt())).willReturn(false);

        locationSignalMonitor.detectLoss();

        then(eventPublisher).shouldHaveNoInteractions();
        then(locationSignalAlertManager).should(never()).release(anyString(), any());
    }

    @Test
    @DisplayName("알림 처리에 실패하면 선점을 풀어 다음 주기에 다시 시도할 수 있게 한다")
    void detectLoss_releases_claim_when_alert_fails() {
        Gps lastGps = givenWardSilentForAnHour();
        given(locationSignalAlertManager.claim(LocationFixture.WARD_KEY, lastGps.recordedAt())).willReturn(true);
        willThrow(new IllegalStateException("fcm unavailable"))
                .given(eventPublisher).publishEvent(any(LocationSignalLostEvent.class));

        assertThatCode(() -> locationSignalMonitor.detectLoss()).doesNotThrowAnyException();

        then(locationSignalAlertManager).should().release(LocationFixture.WARD_KEY, lastGps.recordedAt());
    }

    // The monitor reads the system clock, so the silence is set well inside the 20m–24h window to stay deterministic.
    private Gps givenWardSilentForAnHour() {
        Gps lastGps = LocationFixture.createGpsReceivedAt(LocalDateTime.now().minusHours(1));
        given(careRelationshipReader.findAllWardMemberKeys()).willReturn(List.of(LocationFixture.WARD_KEY));
        given(gpsLatestCacheManager.findAll(List.of(LocationFixture.WARD_KEY)))
                .willReturn(Map.of(LocationFixture.WARD_KEY, lastGps));
        return lastGps;
    }
}
