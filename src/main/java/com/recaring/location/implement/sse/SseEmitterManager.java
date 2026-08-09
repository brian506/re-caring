package com.recaring.location.implement.sse;

import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import com.recaring.location.vo.Gps;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5분
    private static final long POLL_INTERVAL_MS = 10_000L;   // GPS 수신 주기(30s) 이내로 설정하여 불필요한 Redis 조회 최소화
    private static final String EVENT_NAME = "location";

    private final GpsLatestCacheManager gpsLatestCacheManager;
    private final GpsHistoryManager gpsHistoryManager;
    private final Executor ssePollExecutor;

    private final AtomicInteger activeConnections = new AtomicInteger();
    private final Counter sendFailures;
    private final Counter removedCompletion;
    private final Counter removedTimeout;
    private final Counter removedError;

    public SseEmitterManager(MeterRegistry registry,
                             GpsLatestCacheManager gpsLatestCacheManager,
                             GpsHistoryManager gpsHistoryManager,
                             @Qualifier("ssePollExecutor") Executor ssePollExecutor) {
        this.gpsLatestCacheManager = gpsLatestCacheManager;
        this.gpsHistoryManager = gpsHistoryManager;
        this.ssePollExecutor = ssePollExecutor;

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

        ssePollExecutor.execute(() -> pollLoop(emitter, wardKey, active));

        return emitter;
    }

    private void pollLoop(SseEmitter emitter, String wardKey, AtomicBoolean active) {
        Gps lastSent = null;
        try {
            lastSent = sendInitial(emitter, wardKey);
            while (active.get()) {
                Thread.sleep(POLL_INTERVAL_MS);
                if (!active.get()) {
                    break;
                }
                Optional<Gps> latest = gpsLatestCacheManager.find(wardKey);
                if (latest.isPresent() && !latest.get().equals(lastSent)) {
                    emitter.send(SseEmitter.event().name(EVENT_NAME).data(latest.get()));
                    lastSent = latest.get();
                } else {
                    emitter.send(SseEmitter.event().comment("")); // heartbeat: prevent ALB idle timeout (60s)
                }
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

    // 연결 직후 1회. 캐시가 비어 있으면(WARD가 TTL 이상 GPS를 보내지 않음) DB의 마지막 위치로 대체한다.
    // 폴링 중에는 캐시만 본다 — 매 주기 DB를 치면 연결 수에 비례해 부하가 늘기 때문이다.
    private Gps sendInitial(SseEmitter emitter, String wardKey) throws IOException {
        Optional<Gps> initial = gpsLatestCacheManager.find(wardKey)
                .or(() -> gpsHistoryManager.findLatest(wardKey));
        if (initial.isEmpty()) {
            emitter.send(SseEmitter.event().comment(""));
            return null;
        }
        emitter.send(SseEmitter.event().name(EVENT_NAME).data(initial.get()));
        return initial.get();
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
