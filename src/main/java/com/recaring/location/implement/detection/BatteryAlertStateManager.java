package com.recaring.location.implement.detection;

import com.recaring.location.vo.BatteryAlertState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 베터리 알림 중복 방지를 위한 상태 관리 매니저
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatteryAlertStateManager {

    private static final String KEY_PREFIX = "device:battery:";
    private static final String FIELD_LAST_NOTIFIED_THRESHOLD = "lastNotifiedThreshold";
    private static final Duration STATE_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public BatteryAlertState find(String memberKey) {
        Object value = redisTemplate.opsForHash().get(KEY_PREFIX + memberKey, FIELD_LAST_NOTIFIED_THRESHOLD);
        if (value == null) {
            return BatteryAlertState.empty();
        }
        return new BatteryAlertState(toPercent(memberKey, value));
    }

    // 재알림 방지 상태는 남은 값을 지워야 다음 판정이 옛 값을 보지 않는다
    public void save(String memberKey, BatteryAlertState state) {
        String key = KEY_PREFIX + memberKey;
        Integer threshold = state.lastNotifiedThreshold();
        // 임계값을 벗어나면 상태를 지워서 다음에 다시 알릴 수 있도록 리셋
        if (threshold == null) {
            redisTemplate.delete(key);
            return;
        }
        redisTemplate.opsForHash().put(key, FIELD_LAST_NOTIFIED_THRESHOLD, String.valueOf(threshold));
        redisTemplate.expire(key, STATE_TTL);
    }

    public void delete(String memberKey) {
        redisTemplate.delete(KEY_PREFIX + memberKey);
    }

    private Integer toPercent(String memberKey, Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("[배터리 알림 상태 : 값 손상]: memberKey={} | value={}", memberKey, value);
            return null;
        }
    }
}
