package com.recaring.location.implement.detection;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class AnomalyStreamErrorHandler implements ErrorHandler {

    private static final Duration LOG_INTERVAL = Duration.ofMinutes(5);
    private static final Duration MIN_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);
    private static final int MAX_BACKOFF_EXPONENT = 5;

    private final Counter pollErrors;
    private final AtomicLong consecutiveErrors = new AtomicLong();

    private volatile long lastLoggedAtMillis;

    public AnomalyStreamErrorHandler(MeterRegistry registry) {
        this.pollErrors = registry.counter("anomaly.stream.poll.errors");
    }

    @Override
    public void handleError(Throwable throwable) {
        pollErrors.increment();
        long failures = consecutiveErrors.incrementAndGet();
        logThrottled(throwable, failures);
        backOff(failures);
    }

    public void markHealthy() {
        long failures = consecutiveErrors.getAndSet(0);
        if (failures > 0) {
            log.info("[이상탐지 스트림 : 폴링 복구]: failures={}", failures);
        }
    }

    public long consecutiveErrors() {
        return consecutiveErrors.get();
    }

    private void logThrottled(Throwable throwable, long failures) {
        long now = System.currentTimeMillis();
        if (failures == 1) {
            lastLoggedAtMillis = now;
            log.error("[이상탐지 스트림 : 폴링 실패]: failures={} | error={}", failures, rootMessage(throwable));
            return;
        }
        if (now - lastLoggedAtMillis < LOG_INTERVAL.toMillis()) {
            return;
        }
        lastLoggedAtMillis = now;
        log.warn("[이상탐지 스트림 : 폴링 실패 지속]: failures={} | error={}", failures, rootMessage(throwable));
    }

    private void backOff(long failures) {
        int exponent = (int) Math.min(failures - 1, MAX_BACKOFF_EXPONENT);
        long delayMillis = Math.min(MIN_BACKOFF.toMillis() << exponent, MAX_BACKOFF.toMillis());
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
