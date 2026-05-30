package com.recaring.alert.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.alert.vo.InvestigationState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestigationStateReader {

    private static final String KEY_PREFIX = "investigation:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<InvestigationState> findByFingerprint(String fingerprint) {
        String key = buildKey(fingerprint);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, InvestigationState.class));
        } catch (Exception e) {
            log.warn("[Redis 캐시 : 역직렬화 실패]: fingerprint={} | error={}", fingerprint, e.getMessage());
            return Optional.empty();
        }
    }

    private String buildKey(String fingerprint) {
        return KEY_PREFIX + fingerprint;
    }
}
