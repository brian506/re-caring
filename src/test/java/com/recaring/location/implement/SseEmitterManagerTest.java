package com.recaring.location.implement;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.Gps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@DisplayName("SseEmitterManager 단위 테스트")
class SseEmitterManagerTest {

    private final SseEmitterManager manager = new SseEmitterManager();

    @Test
    @DisplayName("connect() 호출 시 SseEmitter가 반환된다")
    void connect_returns_emitter() {
        SseEmitter emitter = manager.connect(LocationFixture.WARD_KEY);

        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("같은 wardKey로 여러 번 connect하면 각기 다른 emitter를 반환한다")
    void connect_returns_distinct_emitters() {
        SseEmitter first = manager.connect(LocationFixture.WARD_KEY);
        SseEmitter second = manager.connect(LocationFixture.WARD_KEY);

        assertThat(first).isNotSameAs(second);
    }

    @Test
    @DisplayName("broadcast() 호출 시 emitter가 없으면 예외 없이 종료된다")
    void broadcast_without_emitters_does_not_throw() {
        Gps gps = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocalDateTime.now());

        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> manager.broadcast("unknown-ward", gps));
    }

    @Test
    @DisplayName("sendInitialEvent() 호출 시 IOException이 발생해도 예외가 전파되지 않는다")
    void sendInitialEvent_swallows_io_exception() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        willThrow(new IOException("connection reset")).given(emitter).send(any(SseEmitter.SseEventBuilder.class));
        Gps gps = new Gps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocalDateTime.now());

        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> manager.sendInitialEvent(emitter, gps));
    }
}
