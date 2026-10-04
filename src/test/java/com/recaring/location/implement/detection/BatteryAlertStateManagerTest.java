package com.recaring.location.implement.detection;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.BatteryAlertState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("배터리 알림 억제 상태 단위 테스트")
class BatteryAlertStateManagerTest {

    @InjectMocks
    private BatteryAlertStateManager manager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    @DisplayName("보호자별로 저장된 무장 값을 보호자 key로 묶어 돌려준다")
    void findAll_returns_state_per_caregiver() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(LocationFixture.BATTERY_ALERT_STATE_KEY)).willReturn(Map.of(
                LocationFixture.GUARDIAN_KEY, "20",
                LocationFixture.OTHER_GUARDIAN_KEY, "50"));

        Map<String, BatteryAlertState> result = manager.findAll(LocationFixture.WARD_KEY);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                LocationFixture.GUARDIAN_KEY, LocationFixture.batteryAlertState(20),
                LocationFixture.OTHER_GUARDIAN_KEY, LocationFixture.batteryAlertState(50)));
    }

    @Test
    @DisplayName("저장된 상태가 없으면 빈 결과를 돌려준다")
    void findAll_returns_empty_when_absent() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(LocationFixture.BATTERY_ALERT_STATE_KEY)).willReturn(Map.of());

        Map<String, BatteryAlertState> result = manager.findAll(LocationFixture.WARD_KEY);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("값이 숫자가 아닌 보호자만 빼고 나머지 상태는 돌려준다")
    void findAll_skips_only_corrupted_value() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(LocationFixture.BATTERY_ALERT_STATE_KEY)).willReturn(Map.of(
                LocationFixture.GUARDIAN_KEY, "not-a-number",
                LocationFixture.OTHER_GUARDIAN_KEY, "50"));

        Map<String, BatteryAlertState> result = manager.findAll(LocationFixture.WARD_KEY);

        assertThat(result).containsExactlyEntriesOf(Map.of(
                LocationFixture.OTHER_GUARDIAN_KEY, LocationFixture.batteryAlertState(50)));
    }

    @Test
    @DisplayName("알린 값은 그 보호자 자리에 저장하고 하루 뒤 만료되게 한다")
    void save_stores_notified_threshold_under_caregiver() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);

        manager.save(LocationFixture.WARD_KEY, LocationFixture.GUARDIAN_KEY,
                LocationFixture.batteryAlertState(LocationFixture.NOTIFIED_THRESHOLD));

        then(hashOperations).should(times(1)).put(
                LocationFixture.BATTERY_ALERT_STATE_KEY, LocationFixture.GUARDIAN_KEY, "20");
        then(redisTemplate).should().expire(LocationFixture.BATTERY_ALERT_STATE_KEY, Duration.ofDays(1));
    }

    @Test
    @DisplayName("무장 해제 상태를 저장하면 그 보호자 자리만 지우고 다른 보호자 상태는 남긴다")
    void save_removes_only_caregiver_field_when_disarmed() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);

        manager.save(LocationFixture.WARD_KEY, LocationFixture.GUARDIAN_KEY, BatteryAlertState.empty());

        then(hashOperations).should(times(1))
                .delete(LocationFixture.BATTERY_ALERT_STATE_KEY, LocationFixture.GUARDIAN_KEY);
        then(redisTemplate).should(never()).delete(LocationFixture.BATTERY_ALERT_STATE_KEY);
    }

    @Test
    @DisplayName("대상자 단위로 지우면 모든 보호자의 상태를 한 번에 지운다")
    void delete_removes_whole_key_of_ward() {
        manager.delete(LocationFixture.WARD_KEY);

        then(redisTemplate).should(times(1)).delete(LocationFixture.BATTERY_ALERT_STATE_KEY);
    }
}
