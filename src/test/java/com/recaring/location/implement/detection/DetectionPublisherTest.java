package com.recaring.location.implement.detection;

import com.recaring.location.fixture.LocationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("이상탐지 GPS 발행 단위 테스트")
class DetectionPublisherTest {

    private static final String STREAM_KEY = "gps-detection";

    @InjectMocks
    private DetectionPublisher detectionPublisher;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    @Mock
    private AnomalyAcknowledgementManager anomalyAcknowledgementManager;

    @Test
    @DisplayName("엔진이 판정에 쓰는 좌표 필드와 기기 속도·측정 시각을 스트림에 올린다")
    void publishes_coordinate_fields() {
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);

        detectionPublisher.publish(LocationFixture.WARD_KEY, LocationFixture.createGps());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        then(streamOperations).should().add(eq(STREAM_KEY), captor.capture(), any(XAddOptions.class));
        assertThat(captor.getValue())
                .containsEntry("ward_member_key", LocationFixture.WARD_KEY)
                .containsEntry("latitude", String.valueOf(LocationFixture.LATITUDE))
                .containsEntry("longitude", String.valueOf(LocationFixture.LONGITUDE))
                .containsEntry("recorded_at", "2026-07-27 10:15:03")
                .containsEntry("accuracy", String.valueOf(LocationFixture.ACCURACY))
                .containsEntry("acknowledged", "false")
                .containsEntry("speed", String.valueOf(LocationFixture.SPEED))
                .containsEntry("measured_at", LocationFixture.MEASURED_AT_TEXT)
                .doesNotContainKey("battery");
    }

    @Test
    @DisplayName("기기가 속도와 측정 시각을 보내지 않았으면 두 필드를 싣지 않는다")
    void omits_speed_and_measured_at_when_device_did_not_report() {
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);

        detectionPublisher.publish(LocationFixture.WARD_KEY, LocationFixture.createGpsWithoutDeviceMotion());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        then(streamOperations).should().add(eq(STREAM_KEY), captor.capture(), any(XAddOptions.class));
        assertThat(captor.getValue())
                .containsEntry("recorded_at", LocationFixture.RECORDED_AT_TEXT)
                .doesNotContainKey("speed")
                .doesNotContainKey("measured_at");
    }

    @Test
    @DisplayName("보호자가 확인한 사건이면 확인 여부를 true로 올린다")
    void publishes_acknowledged_true_when_guardian_acknowledged() {
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        given(anomalyAcknowledgementManager.isAcknowledged(LocationFixture.WARD_KEY)).willReturn(true);

        detectionPublisher.publish(LocationFixture.WARD_KEY, LocationFixture.createGps());

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        then(streamOperations).should().add(eq(STREAM_KEY), captor.capture(), any(XAddOptions.class));
        assertThat(captor.getValue()).containsEntry("acknowledged", "true");
    }

    @Test
    @DisplayName("기기가 정확도를 보고하지 않으면 그 필드를 빼고 올린다")
    void omits_accuracy_when_device_did_not_report_it() {
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);

        detectionPublisher.publish(LocationFixture.WARD_KEY, LocationFixture.createGpsWithAccuracy(null));

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        then(streamOperations).should().add(eq(STREAM_KEY), captor.capture(), any(XAddOptions.class));
        assertThat(captor.getValue()).doesNotContainKey("accuracy");
    }

    @Test
    @DisplayName("Redis가 죽어도 GPS 수신 흐름을 실패시키지 않는다")
    void swallows_redis_failure() {
        given(redisTemplate.<String, String>opsForStream()).willReturn(streamOperations);
        willThrow(new RedisConnectionFailureException("failover"))
                .given(streamOperations).add(eq(STREAM_KEY), any(Map.class), any(XAddOptions.class));

        assertThatCode(() -> detectionPublisher.publish(LocationFixture.WARD_KEY, LocationFixture.createGps()))
                .doesNotThrowAnyException();
    }
}
