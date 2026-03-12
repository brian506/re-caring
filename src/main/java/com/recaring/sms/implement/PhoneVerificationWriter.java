package com.recaring.sms.implement;

import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PhoneVerificationWriter {

    private static final String CODE_KEY_PREFIX = "phone:verify:";
    private static final String VERIFIED_KEY_PREFIX = "phone:verified:";
    private static final long CODE_TTL_MINUTES = 5;
    private static final long VERIFIED_TTL_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;

    public void add(PhoneNumber phone, SmsCode code) {
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone.value(), code.value(), CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void verify(PhoneNumber phone) {
        redisTemplate.delete(CODE_KEY_PREFIX + phone.value());
        redisTemplate.opsForValue().set(VERIFIED_KEY_PREFIX + phone.value(), "true", VERIFIED_TTL_MINUTES, TimeUnit.MINUTES);
    }
}
