package com.recaring.location.implement.detection;

import com.recaring.location.fixture.LocationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("이상탐지 사건 보호자 확인 상태 단위 테스트")
class AnomalyAcknowledgementManagerTest {

    @InjectMocks
    private AnomalyAcknowledgementManager manager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    @DisplayName("현재 사건의 알림을 확인하면 확인 상태로 바꾼다")
    void acknowledges_current_event() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.get(LocationFixture.ANOMALY_ACK_KEY, "eventId")).willReturn(LocationFixture.EVENT_ID);

        boolean applied = manager.acknowledge(LocationFixture.WARD_KEY, LocationFixture.EVENT_ID);

        assertThat(applied).isTrue();
        then(hashOperations).should().put(LocationFixture.ANOMALY_ACK_KEY, "acknowledged", "true");
    }

    @Test
    @DisplayName("지난 사건의 알림을 확인하면 현재 사건의 확인 상태를 바꾸지 않는다")
    void ignores_acknowledgement_of_previous_event() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.get(LocationFixture.ANOMALY_ACK_KEY, "eventId")).willReturn(LocationFixture.EVENT_ID);

        boolean applied = manager.acknowledge(LocationFixture.WARD_KEY, LocationFixture.PREVIOUS_EVENT_ID);

        assertThat(applied).isFalse();
        then(hashOperations).should(never()).put(any(), any(), any());
    }

    @Test
    @DisplayName("진행 중인 사건이 없으면 확인해도 아무것도 바뀌지 않는다")
    void ignores_acknowledgement_without_ongoing_event() {
        given(redisTemplate.<Object, Object>opsForHash()).willReturn(hashOperations);
        given(hashOperations.get(LocationFixture.ANOMALY_ACK_KEY, "eventId")).willReturn(null);

        boolean applied = manager.acknowledge(LocationFixture.WARD_KEY, LocationFixture.EVENT_ID);

        assertThat(applied).isFalse();
        then(hashOperations).should(never()).put(any(), any(), any());
    }
}
