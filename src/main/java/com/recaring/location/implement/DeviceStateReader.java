package com.recaring.location.implement;

import com.recaring.location.vo.DeviceState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceStateReader {

    private static final String KEY_PREFIX = "device:state:";

    private final StringRedisTemplate redisTemplate;

    public DeviceState find(String memberKey) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + memberKey);
        if (value == null) {
            return DeviceState.UNKNOWN;
        }
        try {
            return DeviceState.valueOf(value);
        } catch (IllegalArgumentException e) {
            return DeviceState.UNKNOWN;
        }
    }
}
