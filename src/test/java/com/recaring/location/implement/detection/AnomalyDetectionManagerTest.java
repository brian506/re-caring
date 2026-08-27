package com.recaring.location.implement.detection;

import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.DetectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("이상탐지 결과 저장 단위 테스트")
class AnomalyDetectionManagerTest {

    private static final String EVIDENCE = "근거 문구";

    @InjectMocks
    private AnomalyDetectionManager anomalyDetectionManager;

    @Mock
    private AnomalyDetectionRepository anomalyDetectionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("처음 받은 탐지 결과는 저장하고 알림 이벤트를 발행한다")
    void publishes_event_when_newly_stored() {
        AnomalyAlert alert = LocationFixture.createAnomalyAlert(DetectionType.WANDERING, EVIDENCE);
        given(anomalyDetectionRepository.insertIfAbsent(
                LocationFixture.WARD_KEY, DetectionType.WANDERING.name(), LocationFixture.ANOMALY_SCORE,
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, EVIDENCE))
                .willReturn(1);

        anomalyDetectionManager.record(alert);

        ArgumentCaptor<AnomalyDetectedEvent> captor = ArgumentCaptor.forClass(AnomalyDetectedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().alert()).isSameAs(alert);
    }

    @Test
    @DisplayName("재배달로 다시 받은 탐지 결과는 알림을 두 번 보내지 않는다")
    void does_not_publish_event_when_already_stored() {
        given(anomalyDetectionRepository.insertIfAbsent(
                LocationFixture.WARD_KEY, DetectionType.WANDERING.name(), LocationFixture.ANOMALY_SCORE,
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, EVIDENCE))
                .willReturn(0);

        anomalyDetectionManager.record(LocationFixture.createAnomalyAlert(DetectionType.WANDERING, EVIDENCE));

        then(eventPublisher).should(never()).publishEvent(any(AnomalyDetectedEvent.class));
    }

}
