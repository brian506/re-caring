package com.recaring.location.implement;

import com.recaring.location.vo.Gps;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분
    private static final String EVENT_NAME = "location";

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final Timer broadcastTimer;
    private final Counter sendFailures;
    private final Counter removedCompletion;
    private final Counter removedTimeout;
    private final Counter removedError;

    public SseEmitterManager(MeterRegistry registry) {
        this.broadcastTimer = Timer.builder("sse.broadcast.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.sendFailures      = registry.counter("sse.emitter.send.failures");
        this.removedCompletion = registry.counter("sse.emitter.removed", "reason", "completion");
        this.removedTimeout    = registry.counter("sse.emitter.removed", "reason", "timeout");
        this.removedError      = registry.counter("sse.emitter.removed", "reason", "error");

        Gauge.builder("sse.active.connections", emitters,
                        m -> m.values().stream().mapToInt(List::size).sum())
                .register(registry);
    }

    public SseEmitter connect(String wardKey) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(wardKey, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> { remove(wardKey, emitter); removedCompletion.increment(); });
        emitter.onTimeout(()    -> { remove(wardKey, emitter); removedTimeout.increment(); });
        emitter.onError(e       -> { remove(wardKey, emitter); removedError.increment(); });

        return emitter;
    }

    // SSE 첫 연결 시 전송
    public void sendInitialEvent(SseEmitter emitter, Gps gpsLatest) {
        try {
            emitter.send(SseEmitter.event().name(EVENT_NAME).data(gpsLatest));
        } catch (IOException e) {
            log.debug("[SSE 이벤트 : 초기 전송 실패]: error={}", e.getMessage());
        }
    }

    public void broadcast(String wardKey, Gps gpsLatest) {
        broadcastTimer.record(() -> {
            List<SseEmitter> wardEmitters = emitters.getOrDefault(wardKey, new CopyOnWriteArrayList<>());
            for (SseEmitter emitter : wardEmitters) {
                try {
                    emitter.send(SseEmitter.event().name(EVENT_NAME).data(gpsLatest));
                } catch (IOException e) {
                    log.debug("[SSE 이벤트 : broadcast 전송 실패]: wardKey={} | error={}", wardKey, e.getMessage());
                    sendFailures.increment();
                    remove(wardKey, emitter);
                }
            }
        });
    }

    private void remove(String wardKey, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> wardEmitters = emitters.get(wardKey);
        if (wardEmitters != null) {
            wardEmitters.remove(emitter);
        }
    }
}
