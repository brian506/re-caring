package com.recaring.location.business;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService 단위 테스트")
class LocationServiceTest {

    @InjectMocks
    private LocationService locationService;

    @Mock
    private GpsHistoryWriter gpsHistoryWriter;
    @Mock
    private GpsHistoryReader gpsHistoryReader;
    @Mock
    private GpsLatestCacheWriter gpsLatestCacheWriter;
    @Mock
    private SseEmitterManager sseEmitterManager;
    @Mock
    private LocationValidator locationValidator;

    @Test
    @DisplayName("GPS 수신 시 DB에 저장하고 최신 위치를 캐시에 저장한다")
    void receiveGps_saves_history_and_cache() {
        // When
        locationService.receiveGps(
                LocationFixture.WARD_KEY, LocationFixture.LATITUDE, LocationFixture.LONGITUDE,
                LocationFixture.ACCURACY, LocationFixture.BATTERY);

        // Then
        then(gpsHistoryWriter).should(times(1)).save(
                LocationFixture.WARD_KEY, LocationFixture.LATITUDE, LocationFixture.LONGITUDE,
                LocationFixture.ACCURACY, LocationFixture.BATTERY);
        then(gpsLatestCacheWriter).should(times(1)).save(eq(LocationFixture.WARD_KEY), any());
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
