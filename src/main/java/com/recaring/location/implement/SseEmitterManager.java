package com.recaring.location.implement;

import com.recaring.location.vo.Gps;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5분
    private static final long POLL_INTERVAL_MS = 1000L;     // Redis 최신 GPS 폴링 주기
    private static final String EVENT_NAME = "location";

    private final GpsLatestCacheReader gpsLatestCacheReader;

    private final AtomicInteger activeConnections = new AtomicInteger();
    private final Counter sendFailures;
    private final Counter removedCompletion;
    private final Counter removedTimeout;
    private final Counter removedError;

    public SseEmitterManager(MeterRegistry registry, GpsLatestCacheReader gpsLatestCacheReader) {
        this.gpsLatestCacheReader = gpsLatestCacheReader;

        this.sendFailures      = registry.counter("sse.emitter.send.failures");
        this.removedCompletion = registry.counter("sse.emitter.removed", "reason", "completion");
        this.removedTimeout    = registry.counter("sse.emitter.removed", "reason", "timeout");
        this.removedError      = registry.counter("sse.emitter.removed", "reason", "error");

        Gauge.builder("sse.active.connections", activeConnections, AtomicInteger::get)
                .register(registry);
    }

    // SSE 연결 수립과 동시에 폴링 루프를 시작한다.
    // 초기 최신값 전송과 이후 GPS 갱신 전송이 단일 루프에서 함께 처리된다.
    public SseEmitter connect(String wardKey) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean active = new AtomicBoolean(true);

        emitter.onCompletion(() -> stop(active, removedCompletion));
        emitter.onTimeout(()    -> stop(active, removedTimeout));
        emitter.onError(e       -> stop(active, removedError));

        activeConnections.incrementAndGet();

        // TODO: 가상 스레드 도입 시 연결당 플랫폼 스레드를 AsyncConfig의 Executor(가상 스레드)로 교체
        Thread.ofPlatform()
                .name("sse-poll-", 0)
                .daemon()
                .start(() -> pollLoop(emitter, wardKey, active));

        return emitter;
    }

    private void pollLoop(SseEmitter emitter, String wardKey, AtomicBoolean active) {
        Gps lastSent = null;
        try {
            while (active.get()) {
                Optional<Gps> latest = gpsLatestCacheReader.find(wardKey);
                if (latest.isPresent() && !latest.get().equals(lastSent)) {
                    emitter.send(SseEmitter.event().name(EVENT_NAME).data(latest.get()));
                    lastSent = latest.get();
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (IOException | IllegalStateException e) {
            log.debug("[SSE 이벤트 : 전송 종료]: wardKey={} | error={}", wardKey, e.getMessage());
            sendFailures.increment();
            completeQuietly(emitter, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            completeQuietly(emitter, null);
        }
    }

    private void stop(AtomicBoolean active, Counter reasonCounter) {
        if (active.compareAndSet(true, false)) {
            activeConnections.decrementAndGet();
            reasonCounter.increment();
        }
    }

    private void completeQuietly(SseEmitter emitter, Exception e) {
        try {
            if (e != null) {
                emitter.completeWithError(e);
            } else {
                emitter.complete();
            }
        } catch (Exception ignored) {
            // 이미 종료된 emitter — 무시
        }
    }
}
