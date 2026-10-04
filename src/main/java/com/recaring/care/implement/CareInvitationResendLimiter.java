package com.recaring.care.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import com.recaring.support.response.RetryAfter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareInvitationResendLimiter {

    private static final String KEY_PREFIX = "care:invitation:resend:";
    private static final Duration INTERVAL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public void acquire(String requestKey) {
        String key = KEY_PREFIX + requestKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", INTERVAL);
        if (Boolean.TRUE.equals(acquired)) {
            return;
        }

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long remaining = ttl == null ? 0 : Math.max(ttl, 0);
        log.warn("[케어 요청 재전송 : 간격 제한]: requestKey={} | remaining={}", requestKey, remaining);
        throw new AppException(ErrorType.CARE_REQUEST_RESEND_TOO_SOON, new RetryAfter(remaining));
    }
}
