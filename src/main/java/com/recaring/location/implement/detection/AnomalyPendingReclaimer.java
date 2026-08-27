package com.recaring.location.implement.detection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyPendingReclaimer {

    private static final long SWEEP_INTERVAL_MILLIS = 60 * 60 * 1000L;

    // 기동 직후의 PEL은 전부 이전 프로세스가 남긴 것이다. 빠른 재기동에서도 놓치지 않도록 짧게 잡는다.
    private static final Duration STARTUP_MIN_IDLE = Duration.ofSeconds(10);

    // 정기 청소는 살아 있는 컨슈머가 처리 중인 메시지를 뺏지 않도록 넉넉히 잡는다.
    private static final Duration SWEEP_MIN_IDLE = Duration.ofMinutes(1);

    private static final int BATCH_SIZE = 20;
    private static final int MAX_PASSES = 5;

    // 컨슈머가 매 배달마다 인라인으로 3회 재시도한다. 회수 후에도 실패하면 고쳐질 가망이 없다고 보고 버린다.
    private static final long MAX_DELIVERY_COUNT = 1L;

    private final StringRedisTemplate redisTemplate;
    private final AnomalyDetectionConsumer anomalyDetectionConsumer;

    // 프로세스가 죽어 PEL에 남은 메시지는 알려주는 주체가 없다. 재기동이 유일한 발견 시점이다.
    // 동기로 돌면 회수가 끝날 때까지 기동 완료가 지연되므로 가상 스레드로 넘긴다.
    @Async("detectionRetryExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void reclaimOnStartup() {
        reclaim(STARTUP_MIN_IDLE);
    }

    // 인라인 재시도까지 소진된 메시지를 치우는 용도다. 알림 복구는 재시도와 기동 스캔이 담당한다.
    @Scheduled(fixedDelay = SWEEP_INTERVAL_MILLIS, initialDelay = SWEEP_INTERVAL_MILLIS)
    public void sweep() {
        reclaim(SWEEP_MIN_IDLE);
    }

    private void reclaim(Duration minIdle) {
        for (int pass = 1; pass <= MAX_PASSES; pass++) {
            PendingMessages pendingMessages = findPending(minIdle);
            if (pendingMessages == null || pendingMessages.isEmpty()) {
                return;
            }

            List<RecordId> reclaimTargets = new ArrayList<>();
            for (PendingMessage pendingMessage : pendingMessages) {
                if (pendingMessage.getTotalDeliveryCount() > MAX_DELIVERY_COUNT) {
                    discard(pendingMessage);
                    continue;
                }
                reclaimTargets.add(pendingMessage.getId());
            }

            if (!reclaimTargets.isEmpty()) {
                consume(claim(reclaimTargets, minIdle));
            }
            if (pendingMessages.size() < BATCH_SIZE) {
                return;
            }
        }
        log.warn("[이상탐지 스트림 : PEL 회수 중단]: passes={} | batchSize={}", MAX_PASSES, BATCH_SIZE);
    }

    private PendingMessages findPending(Duration minIdle) {
        try {
            return redisTemplate.opsForStream().pending(
                    AnomalyStreamProperties.STREAM_KEY,
                    AnomalyStreamProperties.GROUP_NAME,
                    Range.unbounded(),
                    BATCH_SIZE,
                    minIdle);
        } catch (DataAccessException e) {
            log.error("[이상탐지 스트림 : PEL 조회 실패]: error={}", e.getMessage());
            return null;
        }
    }

    private List<MapRecord<String, String, String>> claim(List<RecordId> recordIds, Duration minIdle) {
        try {
            List<MapRecord<String, String, String>> records = redisTemplate.<String, String>opsForStream().claim(
                    AnomalyStreamProperties.STREAM_KEY,
                    AnomalyStreamProperties.GROUP_NAME,
                    AnomalyStreamProperties.CONSUMER_NAME,
                    XClaimOptions.minIdle(minIdle).ids(recordIds));
            log.info("[이상탐지 스트림 : PEL 회수]: requested={} | claimed={}", recordIds.size(), records.size());
            return records;
        } catch (DataAccessException e) {
            log.error("[이상탐지 스트림 : PEL 회수 실패]: requested={} | error={}", recordIds.size(), e.getMessage());
            return List.of();
        }
    }

    private void consume(List<MapRecord<String, String, String>> records) {
        for (MapRecord<String, String, String> record : records) {
            // 스트림에서 트림되어 본문이 사라진 엔트리다. 재처리할 것이 없으니 PEL만 비운다.
            if (record.getValue().isEmpty()) {
                acknowledge(record.getId());
                continue;
            }
            anomalyDetectionConsumer.onMessage(record);
        }
    }

    private void discard(PendingMessage pendingMessage) {
        log.error("[이상탐지 스트림 : 재처리 한도 초과 폐기]: recordId={} | deliveryCount={}",
                pendingMessage.getIdAsString(), pendingMessage.getTotalDeliveryCount());
        acknowledge(pendingMessage.getId());
    }

    private void acknowledge(RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(
                AnomalyStreamProperties.STREAM_KEY, AnomalyStreamProperties.GROUP_NAME, recordId);
    }
}
