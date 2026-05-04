package com.recaring.location.implement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.location.vo.Gps;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GpsLatestCacheWriter {

    private static final String KEY_PREFIX = "gps:latest:";
    private static final long TTL_MINUTES = 5;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(String wardMemberKey, Gps gpsLatest) {
        try {
            String value = objectMapper.writeValueAsString(gpsLatest);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + wardMemberKey,
                    value,
                    TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorType.DEFAULT_ERROR);
        }
    }
}
