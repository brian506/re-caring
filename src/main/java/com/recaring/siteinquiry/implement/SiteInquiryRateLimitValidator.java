package com.recaring.siteinquiry.implement;

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
public class SiteInquiryRateLimitValidator {

    private static final String SUBMIT_KEY_PREFIX = "site-inquiry:submit:";
    private static final String VIEW_FAILURE_KEY_PREFIX = "site-inquiry:view-failure:";
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final long SUBMIT_QUOTA = 5;
    private static final long VIEW_FAILURE_QUOTA = 5;

    private final StringRedisTemplate redisTemplate;

    public void validateSubmit(String clientIp) {
        String key = SUBMIT_KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
        if (count <= SUBMIT_QUOTA) {
            return;
        }
        long remaining = remainingSeconds(key);
        log.warn("[웹 문의 : 등록 한도 초과]: clientIp={} | count={} | remaining={}", clientIp, count, remaining);
        throw new AppException(ErrorType.SITE_INQUIRY_SUBMIT_RATE_LIMITED, new RetryAfter(remaining));
    }

    public void validateViewNotLocked(String inquiryKey) {
        String key = VIEW_FAILURE_KEY_PREFIX + inquiryKey;
        String count = redisTemplate.opsForValue().get(key);
        if (count == null || Long.parseLong(count) < VIEW_FAILURE_QUOTA) {
            return;
        }
        long remaining = remainingSeconds(key);
        log.warn("[웹 문의 : 비밀글 조회 잠금]: inquiryKey={} | remaining={}", inquiryKey, remaining);
        throw new AppException(ErrorType.SITE_INQUIRY_VIEW_LOCKED, new RetryAfter(remaining));
    }

    public void recordViewFailure(String inquiryKey) {
        String key = VIEW_FAILURE_KEY_PREFIX + inquiryKey;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
    }

    private long remainingSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl == -1) {
            redisTemplate.expire(key, WINDOW);
            return WINDOW.toSeconds();
        }
        return ttl == null ? 0 : Math.max(ttl, 0);
    }
}
