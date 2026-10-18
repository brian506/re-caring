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
    private static final String DAILY_VIEW_FAILURE_KEY_PREFIX = "site-inquiry:view-failure-daily:";
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration DAILY_WINDOW = Duration.ofHours(24);
    private static final long SUBMIT_QUOTA = 5;
    private static final long VIEW_FAILURE_QUOTA = 5;
    private static final long DAILY_VIEW_FAILURE_QUOTA = 20;

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
        long remaining = remainingSeconds(key, WINDOW);
        log.warn("[웹 문의 : 등록 한도 초과]: clientIp={} | count={} | remaining={}", clientIp, count, remaining);
        throw new AppException(ErrorType.SITE_INQUIRY_SUBMIT_RATE_LIMITED, new RetryAfter(remaining));
    }

    public void reserveViewAttempt(String inquiryKey) {
        reserve(VIEW_FAILURE_KEY_PREFIX + inquiryKey, WINDOW, VIEW_FAILURE_QUOTA, inquiryKey);
        reserve(DAILY_VIEW_FAILURE_KEY_PREFIX + inquiryKey, DAILY_WINDOW, DAILY_VIEW_FAILURE_QUOTA, inquiryKey);
    }

    public void releaseViewAttempt(String inquiryKey) {
        redisTemplate.opsForValue().decrement(VIEW_FAILURE_KEY_PREFIX + inquiryKey);
        redisTemplate.opsForValue().decrement(DAILY_VIEW_FAILURE_KEY_PREFIX + inquiryKey);
    }

    private void reserve(String key, Duration window, long quota, String inquiryKey) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count == 1) {
            redisTemplate.expire(key, window);
        }
        if (count <= quota) {
            return;
        }
        long remaining = remainingSeconds(key, window);
        log.warn("[웹 문의 : 비밀글 조회 잠금]: inquiryKey={} | count={} | remaining={}", inquiryKey, count, remaining);
        throw new AppException(ErrorType.SITE_INQUIRY_VIEW_LOCKED, new RetryAfter(remaining));
    }

    private long remainingSeconds(String key, Duration window) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl == -1) {
            redisTemplate.expire(key, window);
            return window.toSeconds();
        }
        return ttl == null ? 0 : Math.max(ttl, 0);
    }
}
