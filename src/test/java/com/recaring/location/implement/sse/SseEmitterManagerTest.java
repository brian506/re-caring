package com.recaring.location.implement.sse;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.LocationCollectionInterval;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
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
    private final List<SseEmitter> opened = new ArrayList<>();

    // complete 이후 첫 send가 IllegalStateException을 던져 폴링 루프가 끝난다
    @AfterEach
    void closeEmitters() {
        opened.forEach(SseEmitter::complete);
    }

    private SseEmitter connect() {
        SseEmitter emitter = manager.connect(LocationFixture.WARD_KEY, LocationCollectionInterval.THIRTY_SECONDS);
        opened.add(emitter);
        return emitter;
    }

    @Test
    @DisplayName("connect() 호출 시 SseEmitter가 반환된다")
    void connect_returns_emitter() {
        given(gpsLatestCacheManager.findDisplayable(LocationFixture.WARD_KEY)).willReturn(Optional.empty());
        given(gpsHistoryManager.findLatestDisplayable(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        SseEmitter emitter = connect();

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("같은 wardKey로 여러 번 connect하면 각기 다른 emitter를 반환한다")
    void connect_returns_distinct_emitters() {
        given(gpsLatestCacheManager.findDisplayable(LocationFixture.WARD_KEY)).willReturn(Optional.empty());
        given(gpsHistoryManager.findLatestDisplayable(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        SseEmitter first = connect();
        SseEmitter second = connect();

        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("표시용 캐시가 비어 있으면 DB의 마지막 표시 가능 위치를 조회한다")
    void connect_falls_back_to_history_when_cache_is_empty() {
        given(gpsLatestCacheManager.findDisplayable(LocationFixture.WARD_KEY)).willReturn(Optional.empty());
        given(gpsHistoryManager.findLatestDisplayable(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        connect();

        verify(gpsHistoryManager, timeout(1000)).findLatestDisplayable(LocationFixture.WARD_KEY);
    }

    @Test
    @DisplayName("표시용 캐시에 값이 있으면 DB를 조회하지 않는다")
    void connect_does_not_touch_history_when_cache_hits() {
        given(gpsLatestCacheManager.findDisplayable(LocationFixture.WARD_KEY))
                .willReturn(Optional.of(LocationFixture.createGps()));

        connect();

        verify(gpsLatestCacheManager, timeout(1000)).findDisplayable(LocationFixture.WARD_KEY);
        verify(gpsHistoryManager, never()).findLatestDisplayable(LocationFixture.WARD_KEY);
    }
}
