package com.recaring.location.implement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.vo.DetectionPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineTimerStore {

    private static final String LAST_POINT_PREFIX = "device:lastpoint:";
    private static final String DEADLINE_KEY = "device:offline:deadline";
    private static final long LAST_POINT_TTL_MINUTES = 60;

    private static final int SHORT_THRESHOLD_MINUTES = 10;
    private static final int LONG_THRESHOLD_MINUTES = 15;
    private static final int INTERVAL_BOUNDARY_SECONDS = 120;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void arm(String memberKey, DetectionPoint point, int collectionIntervalSeconds) {
        String value;
        try {
            value = objectMapper.writeValueAsString(point);
        } catch (JsonProcessingException e) {
            log.error("[오프라인 타이머 : 직렬화 실패]: memberKey={} | error={}", memberKey, e.getMessage());
            return;
        }
        redisTemplate.opsForValue().set(
                LAST_POINT_PREFIX + memberKey, value, LAST_POINT_TTL_MINUTES, TimeUnit.MINUTES);

        long deadlineEpoch = point.recordedAt()
                .plusMinutes(resolveThresholdMinutes(collectionIntervalSeconds))
                .toEpochSecond(ZoneOffset.UTC);
        redisTemplate.opsForZSet().add(DEADLINE_KEY, memberKey, deadlineEpoch);
    }

    public Set<String> findDue(LocalDateTime now) {
        long nowEpoch = now.toEpochSecond(ZoneOffset.UTC);
        return redisTemplate.opsForZSet().rangeByScore(DEADLINE_KEY, Double.NEGATIVE_INFINITY, nowEpoch);
    }

    public Optional<DetectionPoint> readLastPoint(String memberKey) {
        String value = redisTemplate.opsForValue().get(LAST_POINT_PREFIX + memberKey);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, DetectionPoint.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void disarm(String memberKey) {
        redisTemplate.opsForZSet().remove(DEADLINE_KEY, memberKey);
    }

    private int resolveThresholdMinutes(int collectionIntervalSeconds) {
        return collectionIntervalSeconds <= INTERVAL_BOUNDARY_SECONDS
                ? SHORT_THRESHOLD_MINUTES
                : LONG_THRESHOLD_MINUTES;
    }
}
