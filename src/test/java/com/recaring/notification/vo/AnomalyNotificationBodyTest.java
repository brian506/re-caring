package com.recaring.notification.vo;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.AnomalyAlertKind;
import com.recaring.location.vo.AnomalyReasonCode;
import com.recaring.location.vo.DetectionType;
import com.recaring.notification.fixture.NotificationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("이상탐지 알림 문구 조립 단위 테스트")
class AnomalyNotificationBodyTest {

    @ParameterizedTest(name = "평소 {0}분이면 \"{1}\"")
    @CsvSource({
            "40, 평소 약 40분",
            "60, 평소 약 1시간",
            "90, 평소 약 1시간 30분",
            "200, 평소 약 3시간 20분"
    })
    @DisplayName("평소 체류 시간을 시간·분으로 바꿔 보여준다")
    void shows_usual_dwell_time_in_hours_and_minutes(int usualMin, String expectedDetail) {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.DWELL_KNOWN_LONG,
                LocationFixture.anomalyMeasures(null, usualMin, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 평소보다 오래 한 곳에 머물고 계십니다 (" + expectedDetail + ").");
    }

    @ParameterizedTest(name = "지속 {0}분이면 \"{1}\"")
    @CsvSource({
            "20, 지속 20분",
            "60, 지속 1시간",
            "200, 지속 3시간 20분"
    })
    @DisplayName("지속 시간을 시간·분으로 바꿔 보여준다")
    void shows_duration_in_hours_and_minutes(int durationMin, String expectedDetail) {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.DWELL_UNKNOWN,
                LocationFixture.anomalyMeasures(durationMin, null, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 낯선 곳에 머물고 계십니다 (" + expectedDetail + ").");
    }

    @Test
    @DisplayName("지속 시간과 평소 체류 시간이 함께 있으면 둘 다 보여준다")
    void shows_duration_with_usual_dwell_time() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.DWELL_KNOWN_LONG,
                LocationFixture.anomalyMeasures(135, 90, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 평소보다 오래 한 곳에 머물고 계십니다 (지속 2시간 15분, 평소 약 1시간 30분).");
    }

    @Test
    @DisplayName("차량 이동에 지속 시간이 오면 속도와 함께 보여준다")
    void shows_vehicle_speed_with_duration() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.SPEED_ANOMALY, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.VEHICLE,
                LocationFixture.anomalyMeasures(200, null, 41, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 차량으로 이동 중입니다 (약 41km/h, 지속 3시간 20분).");
    }

    @Test
    @DisplayName("차량 이동은 속도를 함께 보여준다")
    void shows_vehicle_speed() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.SPEED_ANOMALY, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.VEHICLE,
                LocationFixture.anomalyMeasures(null, null, 92, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 차량으로 이동 중입니다 (약 92km/h).");
    }

    @Test
    @DisplayName("차량 속도가 없으면 괄호 없이 보여준다")
    void omits_vehicle_speed_when_absent() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.SPEED_ANOMALY, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.VEHICLE,
                LocationFixture.anomalyMeasures(null, null, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 차량으로 이동 중입니다.");
    }

    @Test
    @DisplayName("야간 외출은 외출 시각을 함께 보여준다")
    void shows_night_outing_hour() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.TIME_ANOMALY, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.NIGHT_OUTING,
                LocationFixture.anomalyMeasures(null, null, null, 3)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 야간(3시경)에 외출하셨습니다.");
    }

    @ParameterizedTest
    @EnumSource(value = AnomalyReasonCode.class, names = {"DWELL_UNKNOWN", "DWELL_AFTER_VEHICLE"})
    @DisplayName("낯선 곳 체류는 차에서 내렸는지와 상관없이 같은 문구로 보여준다")
    void uses_same_sentence_for_unknown_place_dwell(AnomalyReasonCode reasonCode) {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.EVENT_START, reasonCode,
                LocationFixture.anomalyMeasures(20, null, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 낯선 곳에 머물고 계십니다 (지속 20분).");
    }

    @Test
    @DisplayName("재알림은 지속 시간을 빼고 보여준다")
    void omits_duration_on_remind() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.ROUTE_DEVIATION, AnomalyAlertKind.REMIND, AnomalyReasonCode.OFF_ROUTE,
                LocationFixture.anomalyMeasures(20, null, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 평소 다니지 않던 곳에 계십니다.");
    }

    @Test
    @DisplayName("차량 이동 재알림은 속도와 지속 시간을 빼고 보여준다")
    void omits_speed_and_duration_on_vehicle_remind() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.SPEED_ANOMALY, AnomalyAlertKind.REMIND, AnomalyReasonCode.VEHICLE,
                LocationFixture.anomalyMeasures(200, null, 41, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 차량으로 이동 중입니다.");
    }

    @Test
    @DisplayName("재알림이어도 평소 체류 시간은 보여준다")
    void keeps_usual_dwell_time_on_remind() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.REMIND, AnomalyReasonCode.DWELL_KNOWN_LONG,
                LocationFixture.anomalyMeasures(135, 90, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 평소보다 오래 한 곳에 머물고 계십니다 (평소 약 1시간 30분).");
    }

    @Test
    @DisplayName("도착 지연은 고정 문구로 보여준다")
    void shows_fixed_sentence_for_no_arrival() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(LocationFixture.createEventAnomalyAlert(
                DetectionType.ROUTE_DEVIATION, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.NO_ARRIVAL,
                LocationFixture.anomalyMeasures(null, null, null, null)), NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님이 평소 도착하시던 곳에 아직 도착하지 않으셨습니다.");
    }

    @Test
    @DisplayName("사유 코드가 없으면 기본 문구를 쓴다")
    void falls_back_to_default_without_reason_code() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(
                LocationFixture.createAnomalyAlert(DetectionType.ABNORMAL_DWELLING, null),
                NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님에게 이상 징후가 감지되었어요.");
    }

    @Test
    @DisplayName("사유 코드가 없으면 엔진 문장이 있어도 본문에 쓰지 않는다")
    void ignores_evidence_without_reason_code() {
        AnomalyNotificationBody body = AnomalyNotificationBody.of(
                LocationFixture.createAnomalyAlert(DetectionType.WANDERING, LocationFixture.WANDERING_EVIDENCE),
                NotificationFixture.WARD_NAME);

        assertThat(body.value()).isEqualTo("김소연님에게 이상 징후가 감지되었어요.");
    }
}
