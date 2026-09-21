package com.recaring.location.implement.detection;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class AnomalyStreamMetrics {

    private static final long REFRESH_INTERVAL_MILLIS = 30_000L;
    private static final long INITIAL_DELAY_MILLIS = 10_000L;
    private static final long UNKNOWN = -1L;
    private static final String LAG_FIELD = "lag";
    private static final String ID_SEPARATOR = "-";

    private final StringRedisTemplate redisTemplate;
    private final AnomalyStreamErrorHandler anomalyStreamErrorHandler;

    private final AtomicLong subscriptionActive = new AtomicLong();
    private final AtomicLong lag = new AtomicLong(UNKNOWN);
    private final AtomicLong pending = new AtomicLong(UNKNOWN);
    private final AtomicLong deliveryDelaySeconds = new AtomicLong(UNKNOWN);

    private volatile Subscription subscription;

    public AnomalyStreamMetrics(MeterRegistry registry,
                                StringRedisTemplate redisTemplate,
                                AnomalyStreamErrorHandler anomalyStreamErrorHandler) {
        this.redisTemplate = redisTemplate;
        this.anomalyStreamErrorHandler = anomalyStreamErrorHandler;

        Gauge.builder("anomaly.stream.subscription.active", subscriptionActive, AtomicLong::get)
                .register(registry);
        Gauge.builder("anomaly.stream.lag", lag, AtomicLong::get)
                .register(registry);
        Gauge.builder("anomaly.stream.pending", pending, AtomicLong::get)
                .register(registry);
        Gauge.builder("anomaly.stream.delivery.delay", deliveryDelaySeconds, AtomicLong::get)
                .baseUnit("seconds")
                .register(registry);
        Gauge.builder("anomaly.stream.poll.consecutive.errors",
                        anomalyStreamErrorHandler, AnomalyStreamErrorHandler::consecutiveErrors)
                .register(registry);
    }

    public void bind(Subscription subscription) {
        this.subscription = subscription;
    }

    @Scheduled(fixedDelay = REFRESH_INTERVAL_MILLIS, initialDelay = INITIAL_DELAY_MILLIS)
    public void refresh() {
        subscriptionActive.set(subscription != null && subscription.isActive() ? 1L : 0L);
        refreshGroupMetrics();
    }

    private void refreshGroupMetrics() {
        try {
            Optional<StreamInfo.XInfoGroup> group = findGroup();
            if (group.isEmpty()) {
                markUnknown();
                log.warn("[이상탐지 스트림 : 컨슈머 그룹 없음]: group={}", AnomalyStreamProperties.GROUP_NAME);
                return;
            }

            StreamInfo.XInfoStream stream = redisTemplate.opsForStream().info(AnomalyStreamProperties.STREAM_KEY);
            lag.set(readLag(group.get()));
            pending.set(group.get().pendingCount());
            deliveryDelaySeconds.set(deliveryDelay(stream.lastGeneratedId(), group.get().lastDeliveredId()));
            anomalyStreamErrorHandler.markHealthy();
        } catch (RedisConnectionFailureException e) {
            markUnknown();
            log.warn("[이상탐지 스트림 : 지표 수집 연결 실패]: error={}", e.getMessage());
        } catch (DataAccessException e) {
            markUnknown();
            log.warn("[이상탐지 스트림 : 지표 수집 실패]: error={}", e.getMessage());
        }
    }

    private Optional<StreamInfo.XInfoGroup> findGroup() {
        return redisTemplate.opsForStream()
                .groups(AnomalyStreamProperties.STREAM_KEY)
                .stream()
                .filter(group -> AnomalyStreamProperties.GROUP_NAME.equals(group.groupName()))
                .findFirst();
    }

    private long readLag(StreamInfo.XInfoGroup group) {
        Object rawLag = group.getRaw().get(LAG_FIELD);
        if (rawLag instanceof Number number) {
            return number.longValue();
        }
        return UNKNOWN;
    }

    private long deliveryDelay(String lastGeneratedId, String lastDeliveredId) {
        long generatedAt = timestampOf(lastGeneratedId);
        long deliveredAt = timestampOf(lastDeliveredId);
        if (generatedAt == UNKNOWN || deliveredAt == UNKNOWN) {
            return UNKNOWN;
        }
        return Math.max(generatedAt - deliveredAt, 0L) / 1000L;
    }

    private long timestampOf(String recordId) {
        if (recordId == null || recordId.isBlank()) {
            return UNKNOWN;
        }
        String millis = recordId.contains(ID_SEPARATOR)
                ? recordId.substring(0, recordId.indexOf(ID_SEPARATOR))
                : recordId;
        try {
            return Long.parseLong(millis);
        } catch (NumberFormatException e) {
            return UNKNOWN;
        }
    }

    private void markUnknown() {
        lag.set(UNKNOWN);
        pending.set(UNKNOWN);
        deliveryDelaySeconds.set(UNKNOWN);
    }
}
