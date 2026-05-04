package com.recaring.location.implement;

import com.recaring.location.event.GpsEventHandler;
import com.recaring.location.event.GpsReceivedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("GpsEventHandler 단위 테스트")
class GpsEventHandlerTest {

    @InjectMocks
    private GpsEventHandler gpsEventHandler;

    @Mock
    private GpsLatestCacheWriter gpsLatestCacheWriter;
    @Mock
    private SseEmitterManager sseEmitterManager;

    @Test
    @DisplayName("GPS 이벤트 수신 시 Redis 저장, SSE 브로드캐스트가 호출된다")
    void handle_invokes_cache_and_sse() {
        Gps gps = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocalDateTime.now());
        GpsReceivedEvent event = new GpsReceivedEvent(LocationFixture.WARD_KEY, gps);

        gpsEventHandler.handle(event);

        then(gpsLatestCacheWriter).should(times(1)).save(LocationFixture.WARD_KEY, gps);
        then(sseEmitterManager).should(times(1)).broadcast(LocationFixture.WARD_KEY, gps);
    }
}
