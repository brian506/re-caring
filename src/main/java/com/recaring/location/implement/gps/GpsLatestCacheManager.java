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
    private static final String DISPLAY_KEY_PREFIX = "gps:display:";
    private static final long TTL_HOURS = 25;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<Gps> find(String wardMemberKey) {
        return read(KEY_PREFIX, wardMemberKey);
    }

    public Optional<Gps> findDisplayable(String wardMemberKey) {
        return read(DISPLAY_KEY_PREFIX, wardMemberKey);
    }

    private Optional<Gps> read(String keyPrefix, String wardMemberKey) {
        String value = redisTemplate.opsForValue().get(keyPrefix + wardMemberKey);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, Gps.class));
        } catch (JsonProcessingException e) {
            log.warn("[GpsCache : DeserializeFailed]: wardMemberKey={} | error={}", wardMemberKey, e.getOriginalMessage());
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
                log.warn("[GpsCache : DeserializeFailed]: wardMemberKey={} | error={}", wardMemberKeys.get(i), e.getOriginalMessage());
            }
        }
        return result;
    }

    public void save(String wardMemberKey, Gps gpsLatest) {
        write(KEY_PREFIX, wardMemberKey, gpsLatest);
    }

    public void saveDisplayable(String wardMemberKey, Gps gps) {
        write(DISPLAY_KEY_PREFIX, wardMemberKey, gps);
    }

    private void write(String keyPrefix, String wardMemberKey, Gps gps) {
        try {
            String value = objectMapper.writeValueAsString(gps);
            redisTemplate.opsForValue().set(
                    keyPrefix + wardMemberKey,
                    value,
                    TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException | DataAccessException e) {
            log.warn("[GpsCache : SaveFailed]: wardMemberKey={}", wardMemberKey, e);
        }
    }

    public void delete(String wardMemberKey) {
        redisTemplate.delete(List.of(KEY_PREFIX + wardMemberKey, DISPLAY_KEY_PREFIX + wardMemberKey));
    }
}
