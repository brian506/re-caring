package com.recaring.location.business;

import com.recaring.location.event.GpsReceivedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.*;
import com.recaring.location.vo.Gps;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private GpsLatestCacheReader gpsLatestCacheReader;
    @Mock
    private SseEmitterManager sseEmitterManager;
    @Mock
    private LocationValidator locationValidator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("GPS 수신 시 DB 저장 후 GpsReceivedEvent가 발행된다")
    void receiveGps_saves_and_publishes_event() {
        // When
        locationService.receiveGps(LocationFixture.WARD_KEY, LocationFixture.LATITUDE, LocationFixture.LONGITUDE);

        // Then
        then(locationValidator).should(times(1)).validateWardRole(LocationFixture.WARD_KEY);
        then(gpsHistoryWriter).should(times(1)).save(
                eq(LocationFixture.WARD_KEY),
                eq(LocationFixture.LATITUDE),
                eq(LocationFixture.LONGITUDE)
        );
        then(eventPublisher).should(times(1)).publishEvent(any(GpsReceivedEvent.class));
    }

    @Test
    @DisplayName("WARD가 아닌 회원이 GPS 전송 시 예외가 전파된다")
    void receiveGps_propagates_exception_when_not_ward() {
        willThrow(new AppException(ErrorType.NOT_WARD_MEMBER))
                .given(locationValidator).validateWardRole(LocationFixture.GUARDIAN_KEY);

        assertThatThrownBy(() ->
                locationService.receiveGps(LocationFixture.GUARDIAN_KEY, LocationFixture.LATITUDE, LocationFixture.LONGITUDE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_WARD_MEMBER);

        then(gpsHistoryWriter).should(never()).save(any(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("SSE 연결 시 캐시에 위치 정보가 있으면 초기 이벤트를 전송한다")
    void streamLocation_sends_initial_event_when_cache_exists() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        Gps cached = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, java.time.LocalDateTime.now());
        given(sseEmitterManager.connect(LocationFixture.WARD_KEY)).willReturn(mockEmitter);
        given(gpsLatestCacheReader.find(LocationFixture.WARD_KEY)).willReturn(Optional.of(cached));

        SseEmitter result = locationService.streamLocation(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(mockEmitter);
        then(sseEmitterManager).should(times(1)).sendInitialEvent(mockEmitter, cached);
    }

    @Test
    @DisplayName("SSE 연결 시 캐시에 위치 정보가 없으면 초기 이벤트를 전송하지 않는다")
    void streamLocation_skips_initial_event_when_cache_empty() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(sseEmitterManager.connect(LocationFixture.WARD_KEY)).willReturn(mockEmitter);
        given(gpsLatestCacheReader.find(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        locationService.streamLocation(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);

        then(sseEmitterManager).should(never()).sendInitialEvent(any(), any());
    }

    @Test
    @DisplayName("케어 관계 없는 보호자가 SSE 연결 시 예외가 전파된다")
    void streamLocation_propagates_exception_when_not_caregiver() {
        willThrow(new AppException(ErrorType.NOT_CARE_RELATED_WARD))
                .given(locationValidator).validateCaregiverAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY);

        assertThatThrownBy(() ->
                locationService.streamLocation(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CARE_RELATED_WARD);

        then(sseEmitterManager).should(never()).connect(any());
    }
}
