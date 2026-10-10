package com.recaring.location.implement.detection;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 진행 중인 이상탐지 사건의 보호자 확인 여부. 탐지 엔진은 마지막 알림 이후 false→true 전환만 확인으로 인정하므로
 * 새 알림마다 false로 되돌리고, 지난 사건의 알림을 확인해도 현재 사건이 확인되지 않도록 사건 식별자를 함께 둔다.
 */
@Component
@RequiredArgsConstructor
public class AnomalyAcknowledgementManager {

    private static final String KEY_PREFIX = "anomaly:ack:";
    private static final String EVENT_ID = "eventId";
    private static final String ACKNOWLEDGED = "acknowledged";
    private static final Duration STATE_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public void reset(String wardKey, String eventId) {
        String key = KEY_PREFIX + wardKey;
        redisTemplate.opsForHash().putAll(key, Map.of(EVENT_ID, eventId, ACKNOWLEDGED, Boolean.FALSE.toString()));
        redisTemplate.expire(key, STATE_TTL);
    }

    public boolean acknowledge(String wardKey, String eventId) {
        String key = KEY_PREFIX + wardKey;
        Object currentEventId = redisTemplate.opsForHash().get(key, EVENT_ID);
        if (!eventId.equals(currentEventId)) {
            return false;
        }
        redisTemplate.opsForHash().put(key, ACKNOWLEDGED, Boolean.TRUE.toString());
        return true;
    }

    public boolean isAcknowledged(String wardKey) {
        Object value = redisTemplate.opsForHash().get(KEY_PREFIX + wardKey, ACKNOWLEDGED);
        return Boolean.TRUE.toString().equals(value);
    }

    public void delete(String wardKey) {
        redisTemplate.delete(KEY_PREFIX + wardKey);
    }
}
