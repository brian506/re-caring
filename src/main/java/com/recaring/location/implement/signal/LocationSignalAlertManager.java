package com.recaring.location.implement.signal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LocationSignalAlertManager {

    private static final String KEY_PREFIX = "location:signal-lost:";
    private static final Duration TTL = Duration.ofHours(25);

    private final StringRedisTemplate redisTemplate;

    public boolean claim(String wardMemberKey, LocalDateTime lastReceivedAt) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key(wardMemberKey, lastReceivedAt), "1", TTL));
    }

    public void release(String wardMemberKey, LocalDateTime lastReceivedAt) {
        redisTemplate.delete(key(wardMemberKey, lastReceivedAt));
    }

    private String key(String wardMemberKey, LocalDateTime lastReceivedAt) {
        return KEY_PREFIX + wardMemberKey + ":" + lastReceivedAt;
    }
}
