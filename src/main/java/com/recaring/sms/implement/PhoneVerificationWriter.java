package com.recaring.sms.implement;

import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PhoneVerificationWriter {

    private static final String CODE_KEY_PREFIX = "phone:verify:";
    private static final String TOKEN_KEY_PREFIX = "phone:token:";
    private static final long CODE_TTL_MINUTES = 5;
    private static final long TOKEN_TTL_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;

    public void add(PhoneNumber phone, SmsCode code) {
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone.value(), code.value(), CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public String verify(PhoneNumber phone) {
        String token = UUID.randomUUID().toString();
        redisTemplate.delete(CODE_KEY_PREFIX + phone.value());
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, phone.value(), TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    public void deleteToken(String token) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + token);
    }
}
