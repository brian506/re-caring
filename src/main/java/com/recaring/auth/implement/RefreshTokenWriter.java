package com.recaring.auth.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenWriter {

    private static final String KEY_PREFIX = "refresh:token:";

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration; // ms 단위

    private final StringRedisTemplate redisTemplate;

    public void save(String refreshToken, String memberKey) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + refreshToken,
                memberKey,
                refreshExpiration,
                TimeUnit.MILLISECONDS
        );
    }

    public void delete(String refreshToken) {
        redisTemplate.delete(KEY_PREFIX + refreshToken);
    }
}
