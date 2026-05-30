package com.recaring.alert.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recaring.alert.dataaccess.entity.InvestigationStatus;
import com.recaring.alert.vo.InvestigationState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestigationStateWriter {

    private static final String KEY_PREFIX = "investigation:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void saveRunning(String fingerprint, String threadTs) {
        save(fingerprint, new InvestigationState(threadTs, InvestigationStatus.RUNNING, Instant.now(), List.of()));
    }

    public void save(String fingerprint, InvestigationState state) {
        String key = buildKey(fingerprint);
        try {
            String json = objectMapper.writeValueAsString(state);
            stringRedisTemplate.opsForValue().set(key, json, TTL);
            log.info("[Redis 캐시 : 저장 완료]: fingerprint={}", fingerprint);
        } catch (Exception e) {
            log.error("[Redis 캐시 : 직렬화 실패]: fingerprint={} | error={}", fingerprint, e.getMessage());
        }
    }

    public void delete(String fingerprint) {
        String key = buildKey(fingerprint);
        stringRedisTemplate.delete(key);
        log.info("[Redis 캐시 : 삭제 완료]: fingerprint={}", fingerprint);
    }

    private String buildKey(String fingerprint) {
        return KEY_PREFIX + fingerprint;
    }
}
