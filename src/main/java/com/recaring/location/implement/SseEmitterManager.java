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
    private static final long POLL_INTERVAL_MS = 10_000L;   // GPS 수신 주기(30s) 이내로 설정하여 불필요한 Redis 조회 최소화
    private static final String EVENT_NAME = "location";

    private final GpsLatestCacheReader gpsLatestCacheReader;

    private final AtomicInteger activeConnections = new AtomicInteger();
    private final Counter sendFailures;
    private final Counter removedCompletion;
    private final Counter removedTimeout;
    private final Counter removedError;

    public SseEmitterManager(MeterRegistry registry,
                             GpsLatestCacheReader gpsLatestCacheReader) {
        this.gpsLatestCacheReader = gpsLatestCacheReader;

        this.sendFailures      = registry.counter("sse.emitter.send.failures");
        this.removedCompletion = registry.counter("sse.emitter.removed", "reason", "completion");
        this.removedTimeout    = registry.counter("sse.emitter.removed", "reason", "timeout");
        this.removedError      = registry.counter("sse.emitter.removed", "reason", "error");

        Gauge.builder("sse.active.connections", activeConnections, AtomicInteger::get)
                .register(registry);
    }

    // SSE 연결 수립과 동시에 폴링 루프를 시작한다.
    // 초기함최신값 전송과 이후 GPS 갱신 전송이 단일 루프에서 함께 처리된다.
    // todo 5분이 지나면 프론트에서 자동으로 재연결하느 로직 있어야 함
    public SseEmitter connect(String wardKey) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean active = new AtomicBoolean(true);

        emitter.onCompletion(() -> stop(active, removedCompletion));
        emitter.onTimeout(()    -> { stop(active, removedTimeout); completeQuietly(emitter, null); });
        emitter.onError(e       -> stop(active, removedError)); // error already signaled — do NOT call completeWithError, it re-triggers Tomcat error dispatch

        activeConnections.incrementAndGet();

        // 가상 스레드 롤백: 연결당 플랫폼 스레드로 폴링 루프 실행 (메모리 비교 테스트용)
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
                } else {
                    emitter.send(SseEmitter.event().comment("")); // heartbeat: prevent ALB idle timeout (60s)
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (IOException | IllegalStateException e) {
            log.debug("[SSE 이벤트 : 전송 종료]: wardKey={} | error={}", wardKey, e.getMessage());
            sendFailures.increment();
            stop(active, removedError);
            completeQuietly(emitter, null); // client disconnect — complete gracefully, not with error (avoids Tomcat error dispatch)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop(active, removedCompletion);
            completeQuietly(emitter, null);
        } catch (Exception e) {
            log.warn("[SSE 이벤트 : 예상치 못한 종료]: wardKey={} | error={}", wardKey, e.getMessage(), e);
            sendFailures.increment();
            stop(active, removedError);
            completeQuietly(emitter, null); // complete gracefully — completeWithError triggers Tomcat error dispatch on committed response
        }
    }

    private void stop(AtomicBoolean active, Counter reasonCounter) {
        if (active.compareAndSet(true, false)) {
            activeConnections.decrementAndGet();
            reasonCounter.increment();
        }
    }

    private void completeQuietly(SseEmitter emitter, Throwable e) {
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
