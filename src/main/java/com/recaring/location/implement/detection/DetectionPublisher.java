package com.recaring.location.implement.detection;

import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionPublisher {

    private static final String STREAM_KEY = "gps-detection";

    // 1시간이 지나도록 처리되지 못한 메시지는 삭제 (최대 보존 기간 1시간)
    private static final Duration RETENTION = Duration.ofHours(1);

    // toString()은 초가 0이면 초를 생략하고(T14:03) now()는 마이크로초를 단다. 길이를 고정한다.
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate redisTemplate;

    public void publish(String wardMemberKey, Gps gps) {
        try {
            redisTemplate.<String, String>opsForStream()
                    .add(STREAM_KEY, toFields(wardMemberKey, gps), trimBeyondRetention());
        } catch (DataAccessException e) {
            log.warn("[이상탐지 Stream : 발행 실패]: wardMemberKey={} | error={}", wardMemberKey, e.getMessage());
        }
    }

    // 덜 남기는 방향의 오차가 아니므로 안전하고, 매 XADD의 트리밍 비용을 아낀다.
    private XAddOptions trimBeyondRetention() {
        long cutoffEpochMilli = System.currentTimeMillis() - RETENTION.toMillis();
        return XAddOptions.none()
                .minId(RecordId.of(cutoffEpochMilli, 0L))
                .approximateTrimming(true);
    }

    private Map<String, String> toFields(String wardMemberKey, Gps gps) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("ward_member_key", wardMemberKey);
        fields.put("latitude", String.valueOf(gps.latitude()));
        fields.put("longitude", String.valueOf(gps.longitude()));
        fields.put("recorded_at", TIMESTAMP_FORMAT.format(gps.recordedAt()));
        putIfPresent(fields, "accuracy", gps.accuracy());
        return fields;
    }

    private void putIfPresent(Map<String, String> fields, String key, Number value) {
        if (value != null) {
            fields.put(key, String.valueOf(value));
        }
    }
}
