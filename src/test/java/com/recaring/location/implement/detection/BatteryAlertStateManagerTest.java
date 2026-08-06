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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatteryAlertStateManager 단위 테스트")
class BatteryAlertStateManagerTest {

    @InjectMocks
    private BatteryAlertStateManager manager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    @DisplayName("무장된 선택값이 저장되어 있으면 그 값을 담은 상태를 반환한다")
    void find_returns_armed_threshold() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.get(LocationFixture.BATTERY_ALERT_STATE_KEY, LocationFixture.LAST_NOTIFIED_THRESHOLD_FIELD))
                .willReturn("20");

        BatteryAlertState result = manager.find(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(new BatteryAlertState(LocationFixture.NOTIFIED_THRESHOLD));
    }

    @Test
    @DisplayName("저장된 상태가 없으면 무장 해제 상태를 반환한다")
    void find_returns_empty_when_absent() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.get(LocationFixture.BATTERY_ALERT_STATE_KEY, LocationFixture.LAST_NOTIFIED_THRESHOLD_FIELD))
                .willReturn(null);

        BatteryAlertState result = manager.find(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(BatteryAlertState.empty());
    }

    @Test
    @DisplayName("저장된 값이 숫자가 아니면 예외 없이 무장 해제 상태를 반환한다")
    void find_returns_empty_when_value_corrupted() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.get(LocationFixture.BATTERY_ALERT_STATE_KEY, LocationFixture.LAST_NOTIFIED_THRESHOLD_FIELD))
                .willReturn("not-a-number");

        BatteryAlertState result = manager.find(LocationFixture.WARD_KEY);

        assertThat(result).isEqualTo(BatteryAlertState.empty());
    }

    @Test
    @DisplayName("선택값을 알렸으면 그 값을 무장 상태로 저장한다")
    void save_stores_notified_threshold() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);

        manager.save(LocationFixture.WARD_KEY, new BatteryAlertState(LocationFixture.NOTIFIED_THRESHOLD));

        then(hashOperations).should(times(1)).put(
                LocationFixture.BATTERY_ALERT_STATE_KEY,
                LocationFixture.LAST_NOTIFIED_THRESHOLD_FIELD,
                "20");
        then(redisTemplate).should(never()).delete(LocationFixture.BATTERY_ALERT_STATE_KEY);
    }

    @Test
    @DisplayName("무장 해제 상태를 저장하면 키를 삭제해 다음 판정이 옛 값을 보지 않게 한다")
    void save_deletes_key_when_disarmed() {
        manager.save(LocationFixture.WARD_KEY, BatteryAlertState.empty());

        then(redisTemplate).should(times(1)).delete(LocationFixture.BATTERY_ALERT_STATE_KEY);
        then(redisTemplate).should(never()).opsForHash();
    }

    @Test
    @DisplayName("탈퇴 등으로 상태를 지우면 키를 삭제한다")
    void delete_removes_key() {
        manager.delete(LocationFixture.WARD_KEY);

        then(redisTemplate).should(times(1)).delete(LocationFixture.BATTERY_ALERT_STATE_KEY);
    }
}
