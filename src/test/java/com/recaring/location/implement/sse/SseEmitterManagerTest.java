package com.recaring.location.implement.sse;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DisplayName("SseEmitterManager 단위 테스트")
class SseEmitterManagerTest {

    private final GpsLatestCacheManager gpsLatestCacheManager = mock(GpsLatestCacheManager.class);
    private final GpsHistoryManager gpsHistoryManager = mock(GpsHistoryManager.class);
    private final SseEmitterManager manager = new SseEmitterManager(
            new SimpleMeterRegistry(),
            gpsLatestCacheManager,
            gpsHistoryManager,
            Executors.newVirtualThreadPerTaskExecutor()
    );

    @Test
    @DisplayName("connect() 호출 시 SseEmitter가 반환된다")
    void connect_returns_emitter() {
        given(gpsLatestCacheManager.find(LocationFixture.WARD_KEY)).willReturn(Optional.empty());
        given(gpsHistoryManager.findLatest(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        SseEmitter emitter = manager.connect(LocationFixture.WARD_KEY);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("같은 wardKey로 여러 번 connect하면 각기 다른 emitter를 반환한다")
    void connect_returns_distinct_emitters() {
        given(gpsLatestCacheManager.find(LocationFixture.WARD_KEY)).willReturn(Optional.empty());
        given(gpsHistoryManager.findLatest(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        SseEmitter first = manager.connect(LocationFixture.WARD_KEY);
        SseEmitter second = manager.connect(LocationFixture.WARD_KEY);

        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("캐시가 비어 있으면 DB의 마지막 위치를 조회한다")
    void connect_falls_back_to_history_when_cache_is_empty() {
        given(gpsLatestCacheManager.find(LocationFixture.WARD_KEY)).willReturn(Optional.empty());
        given(gpsHistoryManager.findLatest(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        manager.connect(LocationFixture.WARD_KEY);

        verify(gpsHistoryManager, timeout(1000)).findLatest(LocationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("캐시에 값이 있으면 DB를 조회하지 않는다")
    void connect_does_not_touch_history_when_cache_hits() {
        given(gpsLatestCacheManager.find(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(LocationFixture.createGps()));

        manager.connect(LocationFixture.WARD_KEY);

        verify(gpsLatestCacheManager, timeout(1000)).find(LocationFixture.WARD_KEY);
        verify(gpsHistoryManager, never()).findLatest(LocationFixture.WARD_KEY);
    }
}
