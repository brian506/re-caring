package com.recaring.alert.implement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
public class AlertRetryHandler {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    public <T> T executeWithRetry(Supplier<T> action, String operationName) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= DEFAULT_MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("[재시도 : 실패]: operation={} | attempt={} | error={}", operationName, attempt, e.getMessage());
                if (attempt < DEFAULT_MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry delay", ie);
                    }
                }
            }
        }
        log.error("[재시도 : 최대 횟수 초과]: operation={}", operationName);
        throw new RuntimeException("Max retry attempts exceeded for: " + operationName, lastException);
    }

    public void executeWithRetry(Runnable action, String operationName) {
        executeWithRetry(() -> {
            action.run();
            return null;
        }, operationName);
    }
}
