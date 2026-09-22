package com.recaring.sms.implement;

import com.recaring.common.utils.MaskingUtils;
import com.recaring.sms.vo.PhoneNumber;
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
public class SmsRateLimitValidator {

    private static final String QUOTA_KEY_PREFIX = "sms:quota:";
    private static final Duration QUOTA_WINDOW = Duration.ofHours(1);
    private static final long QUOTA = 5;

    private final StringRedisTemplate redisTemplate;

    public void validate(PhoneNumber phone) {
        String key = QUOTA_KEY_PREFIX + phone.value();
        redisTemplate.opsForValue().setIfAbsent(key, "0", QUOTA_WINDOW);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null || count <= QUOTA) {
            return;
        }

        long remaining = remainingSeconds(key);
        log.warn("[SMS 발송 : 한도 초과]: phone={} | count={} | remaining={}",
                MaskingUtils.maskPhone(phone.value()), count, remaining);
        throw new AppException(ErrorType.SMS_SEND_QUOTA_EXCEEDED, new RetryAfter(remaining));
    }

    private long remainingSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null ? 0 : Math.max(ttl, 0);
    }
}
