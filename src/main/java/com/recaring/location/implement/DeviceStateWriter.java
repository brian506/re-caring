package com.recaring.location.implement;

import com.recaring.location.vo.DeviceState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceStateWriter {

    private static final String KEY_PREFIX = "device:state:";

    private final StringRedisTemplate redisTemplate;

    public void save(String memberKey, DeviceState state) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memberKey, state.name());
    }
}
