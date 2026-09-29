package com.recaring.location.implement.gps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GpsLatestCacheManager {

    private static final String KEY_PREFIX = "gps:latest:";
    private static final long TTL_HOURS = 25;

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
            log.warn("[GPS 캐시 : 역직렬화 실패]: wardMemberKey={} | error={}", wardMemberKey, e.getOriginalMessage());
            return Optional.empty();
        }
    }

    public Map<String, Gps> findAll(List<String> wardMemberKeys) {
        if (wardMemberKeys.isEmpty()) {
            return Map.of();
        }
        List<String> values = redisTemplate.opsForValue()
                .multiGet(wardMemberKeys.stream().map(key -> KEY_PREFIX + key).toList());
        if (values == null) {
            return Map.of();
        }

        Map<String, Gps> result = new HashMap<>();
        for (int i = 0; i < wardMemberKeys.size(); i++) {
            String value = values.get(i);
            if (value == null) {
                continue;
            }
            try {
                result.put(wardMemberKeys.get(i), objectMapper.readValue(value, Gps.class));
            } catch (JsonProcessingException e) {
                log.warn("[GPS 캐시 : 역직렬화 실패]: wardMemberKey={} | error={}", wardMemberKeys.get(i), e.getOriginalMessage());
            }
        }
        return result;
    }

    public void save(String wardMemberKey, Gps gpsLatest) {
        try {
            String value = objectMapper.writeValueAsString(gpsLatest);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + wardMemberKey,
                    value,
                    TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException | DataAccessException e) {
            log.warn("[GPS 캐시 : 저장 실패]: wardMemberKey={} | error={}", wardMemberKey, e.getMessage());
        }
    }

    public void delete(String wardMemberKey) {
        redisTemplate.delete(KEY_PREFIX + wardMemberKey);
    }
}
