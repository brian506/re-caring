package com.recaring.location.implement.detection;

import com.recaring.location.vo.AnomalyAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetectionConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final int FIRST_ATTEMPT = 1;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final AnomalyDetectionParser anomalyDetectionParser;
    private final AnomalyDetectionManager anomalyDetectionManager;
    private final Executor detectionRetryExecutor;

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        Optional<AnomalyAlert> alert = anomalyDetectionParser.parse(record.getValue());

        // 해석할 수 없는 메시지는 버림
        if (alert.isEmpty()) {
            acknowledge(record);
            return;
        }

        if (recordAndAcknowledge(record, alert.get(), FIRST_ATTEMPT)) {
            return;
        }

        detectionRetryExecutor.execute(() -> retry(record, alert.get()));
    }

    private void retry(MapRecord<String, String, String> record, AnomalyAlert alert) {
        for (int attempt = FIRST_ATTEMPT + 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (!sleep(RETRY_DELAY.multipliedBy(attempt - FIRST_ATTEMPT))) {
                return;
            }
            if (recordAndAcknowledge(record, alert, attempt)) {
                return;
            }
        }

        log.error("[이상탐지 결과 : 재시도 소진]: wardMemberKey={} | detectionType={} | attempts={}",
                alert.wardMemberKey(), alert.detectionType(), MAX_ATTEMPTS);
    }

    private boolean recordAndAcknowledge(MapRecord<String, String, String> record, AnomalyAlert alert, int attempt) {
        try {
            // 탐지 저장과 알림이 끝난 뒤에 ACK해야 도중에 죽어도 PEL 회수로 복구된다.
            anomalyDetectionManager.record(alert);
        } catch (RuntimeException e) {
            log.error("[이상탐지 결과 : 처리 실패]: wardMemberKey={} | detectionType={} | attempt={} | error={}",
                    alert.wardMemberKey(), alert.detectionType(), attempt, e.getMessage());
            return false;
        }
        acknowledge(record);
        return true;
    }

    private boolean sleep(Duration duration) {
        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        redisTemplate.opsForStream()
                .acknowledge(AnomalyStreamProperties.GROUP_NAME, record);
    }
}
