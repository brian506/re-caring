package com.recaring.location.implement.detection;

import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.AnomalyAlertKind;
import com.recaring.location.vo.AnomalyReasonCode;
import com.recaring.location.vo.DetectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
    private AnomalyAcknowledgementManager anomalyAcknowledgementManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("처음 받은 탐지 결과는 저장하고 알림 이벤트를 발행한다")
    void publishes_event_when_newly_stored() {
        AnomalyAlert alert = LocationFixture.createAnomalyAlert(DetectionType.WANDERING, EVIDENCE);
        given(anomalyDetectionRepository.insertIfAbsent(
                LocationFixture.WARD_KEY, DetectionType.WANDERING.name(), LocationFixture.ANOMALY_SCORE,
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, EVIDENCE,
                null, null, null, null, null, null, null, null, null, null, null))
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
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, EVIDENCE,
                null, null, null, null, null, null, null, null, null, null, null))
                .willReturn(0);

        anomalyDetectionManager.record(LocationFixture.createAnomalyAlert(DetectionType.WANDERING, EVIDENCE));

        then(eventPublisher).should(never()).publishEvent(any(AnomalyDetectedEvent.class));
    }

    @Test
    @DisplayName("새 사건 알림을 저장하면 알림을 보내고 그 사건을 미확인 상태로 되돌린다")
    void resets_acknowledgement_for_the_event_when_new_alert_is_stored() {
        given(anomalyDetectionRepository.insertIfAbsent(
                LocationFixture.WARD_KEY, DetectionType.ABNORMAL_DWELLING.name(), LocationFixture.ANOMALY_SCORE,
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, null,
                AnomalyAlertKind.CATEGORY_ADDED.name(), LocationFixture.EVENT_ID, LocationFixture.EVENT_STARTED_AT,
                null, DetectionType.ABNORMAL_DWELLING.name(), AnomalyReasonCode.DWELL_AFTER_VEHICLE.name(),
                LocationFixture.DWELL_DURATION_MIN, null, null, null, null))
                .willReturn(1);

        anomalyDetectionManager.record(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.CATEGORY_ADDED, AnomalyReasonCode.DWELL_AFTER_VEHICLE,
                LocationFixture.anomalyMeasures(LocationFixture.DWELL_DURATION_MIN, null, null, null)));

        then(anomalyAcknowledgementManager).should().reset(LocationFixture.WARD_KEY, LocationFixture.EVENT_ID);
        then(eventPublisher).should().publishEvent(any(AnomalyDetectedEvent.class));
    }

    @Test
    @DisplayName("사건 종료는 저장하되 알림을 보내지 않고 확인 상태를 지운다")
    void stores_event_end_without_notification_and_clears_acknowledgement() {
        given(anomalyDetectionRepository.insertIfAbsent(
                LocationFixture.WARD_KEY, DetectionType.ROUTE_DEVIATION.name(), LocationFixture.ANOMALY_SCORE,
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, null,
                AnomalyAlertKind.EVENT_END.name(), LocationFixture.EVENT_ID, LocationFixture.EVENT_STARTED_AT,
                LocationFixture.DETECTED_AT, DetectionType.ROUTE_DEVIATION.name(), AnomalyReasonCode.OFF_ROUTE.name(),
                null, null, null, null, null))
                .willReturn(1);

        anomalyDetectionManager.record(LocationFixture.createEventAnomalyAlert(
                DetectionType.ROUTE_DEVIATION, AnomalyAlertKind.EVENT_END, AnomalyReasonCode.OFF_ROUTE,
                LocationFixture.anomalyMeasures(null, null, null, null)));

        then(eventPublisher).should(never()).publishEvent(any(AnomalyDetectedEvent.class));
        then(anomalyAcknowledgementManager).should().delete(LocationFixture.WARD_KEY);
        then(anomalyAcknowledgementManager).should(never()).reset(anyString(), anyString());
    }

    @ParameterizedTest
    @EnumSource(value = AnomalyReasonCode.class, names = {"DWELL_KNOWN_LONG", "DWELL_SAFEZONE_LONG"})
    @DisplayName("아는 장소·안심존 체류의 재알림은 저장만 하고 알림을 보내지 않는다")
    void does_not_publish_remind_for_long_stay(AnomalyReasonCode reasonCode) {
        givenStored(AnomalyAlertKind.REMIND, reasonCode, 1);

        anomalyDetectionManager.record(dwellingAlert(AnomalyAlertKind.REMIND, reasonCode));

        then(eventPublisher).should(never()).publishEvent(any(AnomalyDetectedEvent.class));
    }

    @ParameterizedTest
    @EnumSource(value = AnomalyReasonCode.class, names = {"DWELL_KNOWN_LONG", "DWELL_SAFEZONE_LONG"})
    @DisplayName("아는 장소·안심존 체류의 재알림은 보호자 확인 상태를 미확인으로 되돌리지 않는다")
    void does_not_reset_acknowledgement_on_long_stay_remind(AnomalyReasonCode reasonCode) {
        givenStored(AnomalyAlertKind.REMIND, reasonCode, 1);

        anomalyDetectionManager.record(dwellingAlert(AnomalyAlertKind.REMIND, reasonCode));

        then(anomalyAcknowledgementManager).should(never()).reset(anyString(), anyString());
    }

    @Test
    @DisplayName("아는 장소 체류도 사건의 첫 알림은 보낸다")
    void publishes_first_alert_for_long_stay() {
        givenStored(AnomalyAlertKind.EVENT_START, AnomalyReasonCode.DWELL_KNOWN_LONG, null);

        anomalyDetectionManager.record(dwellingAlert(AnomalyAlertKind.EVENT_START, AnomalyReasonCode.DWELL_KNOWN_LONG));

        then(eventPublisher).should().publishEvent(any(AnomalyDetectedEvent.class));
    }

    @Test
    @DisplayName("낯선 곳 체류의 재알림은 그대로 보낸다")
    void publishes_remind_for_unknown_place_dwelling() {
        givenStored(AnomalyAlertKind.REMIND, AnomalyReasonCode.DWELL_UNKNOWN, 1);

        anomalyDetectionManager.record(dwellingAlert(AnomalyAlertKind.REMIND, AnomalyReasonCode.DWELL_UNKNOWN));

        then(eventPublisher).should().publishEvent(any(AnomalyDetectedEvent.class));
    }

    @Test
    @DisplayName("재배달된 사건 알림은 이미 확인한 사건을 미확인으로 되돌리지 않는다")
    void does_not_reset_acknowledgement_on_redelivery() {
        given(anomalyDetectionRepository.insertIfAbsent(
                LocationFixture.WARD_KEY, DetectionType.TIME_ANOMALY.name(), LocationFixture.ANOMALY_SCORE,
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, null,
                AnomalyAlertKind.REMIND.name(), LocationFixture.EVENT_ID, LocationFixture.EVENT_STARTED_AT,
                null, DetectionType.TIME_ANOMALY.name(), AnomalyReasonCode.NIGHT_OUTING.name(),
                null, null, null, 3, 1))
                .willReturn(0);

        anomalyDetectionManager.record(LocationFixture.createEventAnomalyAlert(
                DetectionType.TIME_ANOMALY, AnomalyAlertKind.REMIND, AnomalyReasonCode.NIGHT_OUTING,
                LocationFixture.anomalyMeasures(null, null, null, 3)));

        then(anomalyAcknowledgementManager).should(never()).reset(anyString(), anyString());
    }

    private AnomalyAlert dwellingAlert(AnomalyAlertKind alertKind, AnomalyReasonCode reasonCode) {
        return LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, alertKind, reasonCode,
                LocationFixture.anomalyMeasures(LocationFixture.DWELL_DURATION_MIN, null, null, null));
    }

    private void givenStored(AnomalyAlertKind alertKind, AnomalyReasonCode reasonCode, Integer remindSeq) {
        given(anomalyDetectionRepository.insertIfAbsent(
                LocationFixture.WARD_KEY, DetectionType.ABNORMAL_DWELLING.name(), LocationFixture.ANOMALY_SCORE,
                LocationFixture.DETECTED_AT, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, null,
                alertKind.name(), LocationFixture.EVENT_ID, LocationFixture.EVENT_STARTED_AT,
                null, DetectionType.ABNORMAL_DWELLING.name(), reasonCode.name(),
                LocationFixture.DWELL_DURATION_MIN, null, null, null, remindSeq))
                .willReturn(1);
    }
}
