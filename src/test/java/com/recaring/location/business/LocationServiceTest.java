package com.recaring.location.business;

import com.recaring.location.event.GpsSavedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.LocationValidator;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.sse.SseEmitterManager;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService 단위 테스트")
class LocationServiceTest {

    @InjectMocks
    private LocationService locationService;

    @Mock
    private GpsHistoryManager gpsHistoryManager;
    @Mock
    private SseEmitterManager sseEmitterManager;
    @Mock
    private LocationValidator locationValidator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("GPS 수신 시 DB에 저장하고 GpsSavedEvent를 발행한다")
    void receiveGps_saves_history_and_publishes_event() {
        // Given
        Gps gps = LocationFixture.createGps();

        // When
        locationService.receiveGps(LocationFixture.WARD_KEY, gps);

        // Then
        then(gpsHistoryManager).should(times(1)).save(LocationFixture.WARD_KEY, gps);
        then(eventPublisher).should(times(1)).publishEvent(any(GpsSavedEvent.class));
    }

    @Test
    @DisplayName("SSE 연결 시 caregiverAccess 검증 후 emitter를 반환한다")
    void streamLocation_returns_emitter_with_caregiver_validation() {
        // Given
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(sseEmitterManager.connect(LocationFixture.WARD_KEY)).willReturn(mockEmitter);

        // When
        SseEmitter result = locationService.streamLocation(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);

        // Then
        assertThat(result).isEqualTo(mockEmitter);
        then(locationValidator).should(times(1))
                .validateCaregiverAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);
    }
}
