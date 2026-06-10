package com.recaring.device.implement;

import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WardDeviceTokenReader {

    private static final String KEY_PREFIX = "deviceToken:";
    private static final Duration TTL = Duration.ofDays(30);

    private final WardDeviceTokenRepository wardDeviceTokenRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public String getByToken(String token) {
        String cached = stringRedisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (cached != null) {
            return cached;
        }

        String wardKey = wardDeviceTokenRepository.findByToken(token)
                .map(entity -> entity.getWardKey())
                .orElseThrow(() -> new AppException(ErrorType.INVALID_DEVICE_TOKEN));

        stringRedisTemplate.opsForValue().set(KEY_PREFIX + token, wardKey, TTL);
        return wardKey;
    }

    public void evict(String token) {
        stringRedisTemplate.delete(KEY_PREFIX + token);
    }
}
