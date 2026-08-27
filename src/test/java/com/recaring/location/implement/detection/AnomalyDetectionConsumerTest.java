package com.recaring.location.implement.detection;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.DetectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("이상탐지 결과 소비 단위 테스트")
class AnomalyDetectionConsumerTest {

    private static final String GROUP_NAME = "recaring-backend";
    private static final RecordId RECORD_ID = RecordId.of("1787800848000-0");

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    @Mock
    private AnomalyDetectionManager anomalyDetectionManager;

    @Mock
    private Executor detectionRetryExecutor;
    
    private final AnomalyDetectionParser anomalyDetectionParser = new AnomalyDetectionParser();

    private AnomalyDetectionConsumer anomalyDetectionConsumer;

    @BeforeEach
    void setUp() {
        anomalyDetectionConsumer = new AnomalyDetectionConsumer(
                redisTemplate, anomalyDetectionParser, anomalyDetectionManager, detectionRetryExecutor);
    }

    @Test
    @DisplayName("탐지 결과를 저장한 뒤에 ACK한다")
    void acknowledges_after_recording() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        MapRecord<String, String, String> record = record(fields());

        // when
        anomalyDetectionConsumer.onMessage(record);

        InOrder inOrder = inOrder(anomalyDetectionManager, streamOperations);
        ArgumentCaptor<AnomalyAlert> captor = ArgumentCaptor.forClass(AnomalyAlert.class);
        inOrder.verify(anomalyDetectionManager).record(captor.capture());
        inOrder.verify(streamOperations).acknowledge(GROUP_NAME, record);

        AnomalyAlert alert = captor.getValue();
        assertThat(alert.wardMemberKey()).isEqualTo(LocationFixture.WARD_KEY);
        assertThat(alert.detectionType()).isEqualTo(DetectionType.WANDERING);
        assertThat(alert.score()).isEqualTo(0.82);
        assertThat(alert.detectedAt()).isEqualTo(LocalDateTime.of(2026, 7, 27, 10, 15, 3));
        assertThat(alert.latitude()).isEqualTo(LocationFixture.LATITUDE);
        assertThat(alert.longitude()).isEqualTo(LocationFixture.LONGITUDE);
        assertThat(alert.evidence()).isEqualTo("{name} 님이 같은 곳을 맴돌고 계십니다.");
    }

    @Test
    @DisplayName("해석할 수 없는 메시지는 저장하지 않고 ACK만 한다")
    void acknowledges_and_drops_unparsable_message() {
        // given
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        Map<String, String> fields = fields();
        fields.put("detection_type", "SIGNAL_LOST");
        MapRecord<String, String, String> record = record(fields);

        // when
        anomalyDetectionConsumer.onMessage(record);

        then(anomalyDetectionManager).should(never()).record(any(AnomalyAlert.class));
        then(streamOperations).should().acknowledge(GROUP_NAME, record);
    }

    @Test
    @DisplayName("저장에 실패하면 ACK하지 않고 재시도로 넘긴다")
    void hands_over_to_retry_without_acknowledging_when_recording_fails() {
        // given
        willThrow(new IllegalStateException("insert failed"))
                .given(anomalyDetectionManager).record(any(AnomalyAlert.class));

        // when
        anomalyDetectionConsumer.onMessage(record(fields()));

        then(streamOperations).should(never()).acknowledge(any(String.class), any(MapRecord.class));
        then(detectionRetryExecutor).should().execute(any(Runnable.class));
    }

    private MapRecord<String, String, String> record(Map<String, String> fields) {
        return MapRecord.create(AnomalyStreamProperties.STREAM_KEY, fields).withId(RECORD_ID);
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
