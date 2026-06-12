package com.recaring.location.implement;

import com.recaring.location.fixture.LocationFixture;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("SseEmitterManager 단위 테스트")
class SseEmitterManagerTest {

    private final GpsLatestCacheReader gpsLatestCacheReader = mock(GpsLatestCacheReader.class);
    private final SseEmitterManager manager = new SseEmitterManager(
            new SimpleMeterRegistry(),
            gpsLatestCacheReader
    );

    @Test
    @DisplayName("connect() 호출 시 SseEmitter가 반환된다")
    void connect_returns_emitter() {
        given(gpsLatestCacheReader.find(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        SseEmitter emitter = manager.connect(LocationFixture.WARD_KEY);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("같은 wardKey로 여러 번 connect하면 각기 다른 emitter를 반환한다")
    void connect_returns_distinct_emitters() {
        given(gpsLatestCacheReader.find(LocationFixture.WARD_KEY)).willReturn(Optional.empty());

        SseEmitter first = manager.connect(LocationFixture.WARD_KEY);
        SseEmitter second = manager.connect(LocationFixture.WARD_KEY);

        assertThat(first).isNotSameAs(second);
    }
}
