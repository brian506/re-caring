package com.recaring.location.implement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.vo.Gps;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GpsLatestCacheReader {

    private static final String KEY_PREFIX = "gps:latest:";

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
}
