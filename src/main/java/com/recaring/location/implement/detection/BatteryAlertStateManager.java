package com.recaring.location.implement.detection;

import com.recaring.location.vo.BatteryAlertState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 배터리 알림 중복 방지 상태. 보호자마다 고른 잔량이 달라 상태도 보호자별로 둔다.
 * 대상자 하나를 한 해시로 묶어(field = 보호자 key) 판정 한 번에 한 번만 읽고, 대상자 탈퇴 시 한 번에 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatteryAlertStateManager {

    private static final String KEY_PREFIX = "device:battery:";
    private static final Duration STATE_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public Map<String, BatteryAlertState> findAll(String wardKey) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(KEY_PREFIX + wardKey);
        Map<String, BatteryAlertState> states = new HashMap<>();
        entries.forEach((caregiverKey, value) -> {
            Integer threshold = toPercent(wardKey, value);
            if (threshold != null) {
                states.put(caregiverKey.toString(), new BatteryAlertState(threshold));
            }
        });
        return states;
    }

    // 재알림 방지 상태는 남은 값을 지워야 다음 판정이 옛 값을 보지 않는다
    public void save(String wardKey, String caregiverKey, BatteryAlertState state) {
        Integer threshold = state.lastNotifiedThreshold();
        if (threshold == null) {
            delete(wardKey, caregiverKey);
            return;
        }
        String key = KEY_PREFIX + wardKey;
        redisTemplate.opsForHash().put(key, caregiverKey, String.valueOf(threshold));
        redisTemplate.expire(key, STATE_TTL);
    }

    public void delete(String wardKey, String caregiverKey) {
        redisTemplate.opsForHash().delete(KEY_PREFIX + wardKey, caregiverKey);
    }

    public void delete(String wardKey) {
        redisTemplate.delete(KEY_PREFIX + wardKey);
    }

    private Integer toPercent(String wardKey, Object value) {
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("[BatteryAlert : StateCorrupted]: wardKey={} | value={}", wardKey, value);
            return null;
        }
    }
}
