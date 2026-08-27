package com.recaring.location.implement.detection;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.DetectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("이상탐지 PEL 회수 단위 테스트")
class AnomalyPendingReclaimerTest {

    private static final String STREAM_KEY = "anomaly-alerts";
    private static final String GROUP_NAME = "recaring-backend";
    private static final RecordId RECORD_ID = RecordId.of("1787800848000-0");
    private static final Duration IDLE = Duration.ofSeconds(30);

    @InjectMocks
    private AnomalyPendingReclaimer anomalyPendingReclaimer;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    @Mock
    private AnomalyDetectionConsumer anomalyDetectionConsumer;

    @Test
    @DisplayName("남아 있던 메시지를 회수해 다시 처리한다")
    void reclaims_and_reprocesses_a_pending_message() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        givenPending(pendingMessage(1L));
        MapRecord<String, String, String> reclaimed = record(fields());
        given(streamOperations.claim(eq(STREAM_KEY), eq(GROUP_NAME), any(String.class), any(XClaimOptions.class)))
                .willReturn(List.of(reclaimed));

        // when
        anomalyPendingReclaimer.sweep();

        then(anomalyDetectionConsumer).should().onMessage(reclaimed);
    }

    @Test
    @DisplayName("이미 재배달된 메시지는 회수하지 않고 버린다")
    void discards_a_message_that_was_already_redelivered() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        givenPending(pendingMessage(2L));

        // when
        anomalyPendingReclaimer.sweep();

        then(anomalyDetectionConsumer).should(never()).onMessage(any());
        then(streamOperations).should().acknowledge(STREAM_KEY, GROUP_NAME, RECORD_ID);
    }

    @Test
    @DisplayName("본문이 사라진 메시지는 재처리하지 않고 PEL만 비운다")
    void acknowledges_a_trimmed_message_without_reprocessing() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        givenPending(pendingMessage(1L));
        given(streamOperations.claim(eq(STREAM_KEY), eq(GROUP_NAME), any(String.class), any(XClaimOptions.class)))
                .willReturn(List.of(record(new HashMap<>())));

        // when
        anomalyPendingReclaimer.sweep();

        then(anomalyDetectionConsumer).should(never()).onMessage(any());
        then(streamOperations).should().acknowledge(STREAM_KEY, GROUP_NAME, RECORD_ID);
    }

    @Test
    @DisplayName("회수할 메시지가 없으면 아무것도 하지 않는다")
    void does_nothing_when_nothing_is_pending() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        givenPending();

        // when
        anomalyPendingReclaimer.sweep();

        then(streamOperations).should(never())
                .claim(any(String.class), any(String.class), any(String.class), any(XClaimOptions.class));
        then(anomalyDetectionConsumer).should(never()).onMessage(any());
    }

    @Test
    @DisplayName("PEL 조회가 실패해도 예외를 전파하지 않는다")
    void swallows_the_exception_when_the_pending_lookup_fails() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        willThrow(new RedisConnectionFailureException("redis down"))
                .given(streamOperations).pending(
                        eq(STREAM_KEY), eq(GROUP_NAME), any(Range.class), anyLong(), any(Duration.class));

        assertThatCode(() -> anomalyPendingReclaimer.sweep()).doesNotThrowAnyException();
        then(anomalyDetectionConsumer).should(never()).onMessage(any());
    }

    @Test
    @DisplayName("회수 자체가 실패해도 예외를 전파하지 않는다")
    void swallows_the_exception_when_the_claim_fails() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        givenPending(pendingMessage(1L));
        willThrow(new RedisConnectionFailureException("redis down"))
                .given(streamOperations).claim(
                        eq(STREAM_KEY), eq(GROUP_NAME), any(String.class), any(XClaimOptions.class));

        assertThatCode(() -> anomalyPendingReclaimer.sweep()).doesNotThrowAnyException();
        then(anomalyDetectionConsumer).should(never()).onMessage(any());
    }

    @Test
    @DisplayName("기동 직후 회수는 정기 청소보다 짧게 기다린 메시지도 가져간다")
    void startup_reclaim_uses_a_shorter_idle_than_the_periodic_sweep() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        givenPending();

        // when
        anomalyPendingReclaimer.reclaimOnStartup();
        anomalyPendingReclaimer.sweep();

        ArgumentCaptor<Duration> captor = ArgumentCaptor.forClass(Duration.class);
        then(streamOperations).should(times(2)).pending(
                eq(STREAM_KEY), eq(GROUP_NAME), any(Range.class), anyLong(), captor.capture());
        assertThat(captor.getAllValues().get(0)).isEqualTo(Duration.ofSeconds(10));
        assertThat(captor.getAllValues().get(1)).isEqualTo(Duration.ofMinutes(1));
    }

    private void givenPending(PendingMessage... messages) {
        given(streamOperations.pending(
                eq(STREAM_KEY), eq(GROUP_NAME), any(Range.class), anyLong(), any(Duration.class)))
                .willReturn(new PendingMessages(GROUP_NAME, Range.unbounded(), List.of(messages)));
    }

    private PendingMessage pendingMessage(long deliveryCount) {
        return new PendingMessage(
                RECORD_ID, Consumer.from(GROUP_NAME, "recaring-test-consumer"), IDLE, deliveryCount);
    }

    private MapRecord<String, String, String> record(Map<String, String> fields) {
        return MapRecord.create(STREAM_KEY, fields).withId(RECORD_ID);
    }

    private Map<String, String> fields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("ward_member_key", LocationFixture.WARD_KEY);
        fields.put("detection_type", DetectionType.WANDERING.name());
        fields.put("score", "0.82");
        fields.put("detected_at", "2026-07-27 10:15:03");
        fields.put("latitude", String.valueOf(LocationFixture.LATITUDE));
        fields.put("longitude", String.valueOf(LocationFixture.LONGITUDE));
        fields.put("evidence", "{name} 님이 같은 곳을 맴돌고 계십니다.");
        return fields;
    }
}
