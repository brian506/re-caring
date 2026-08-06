package com.recaring.location.implement.gps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsLatestCacheManager {

    private static final String KEY_PREFIX = "gps:latest:";
    private static final long TTL_MINUTES = 11;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<Gps> find(String wardMemberKey) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + wardMemberKey);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, Gps.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void save(String wardMemberKey, Gps gpsLatest) {
        try {
            String value = objectMapper.writeValueAsString(gpsLatest);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + wardMemberKey,
                    value,
                    TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException | DataAccessException e) {
            log.warn("[GPS 캐시 : 저장 실패]: wardMemberKey={} | error={}", wardMemberKey, e.getMessage());
        }
    }

    public void delete(String wardMemberKey) {
        redisTemplate.delete(KEY_PREFIX + wardMemberKey);
    }
}
