package com.recaring.notification.implement.listener;

import com.recaring.care.vo.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.AnomalyAlertKind;
import com.recaring.location.vo.AnomalyReasonCode;
import com.recaring.location.vo.DetectionType;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("이상탐지 알림 리스너 단위 테스트")
class AnomalyNotificationListenerTest {

    private AnomalyNotificationListener anomalyNotificationListener;

    @Mock
    private CareRelationshipReader careRelationshipReader;
    @Mock
    private NotificationSettingRepository notificationSettingRepository;
    @Mock
    private NotificationSendManager notificationSendManager;
    @Mock
    private MemberReader memberReader;

    @BeforeEach
    void setUp() {
        anomalyNotificationListener = new AnomalyNotificationListener(
                careRelationshipReader,
                new NotificationSettingReader(notificationSettingRepository),
                notificationSendManager,
                memberReader);
    }

    @Test
    @DisplayName("탐지 근거 문구의 이름 자리에 대상자 이름을 채워 보낸다")
    void fills_ward_name_into_evidence() {
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.WANDERING, "{name} 님이 같은 곳을 맴돌고 계십니다."));

        assertThat(capturedBody()).isEqualTo("김소연 님이 같은 곳을 맴돌고 계십니다.");
    }

    @Test
    @DisplayName("보호자와 관계자를 역할별로 나눠 알림 발송을 위임한다")
    void splits_recipients_by_care_role() {
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.SPEED_ANOMALY, "{name} 님이 차량 속도로 이동 중입니다."));

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                DetectionType.SPEED_ANOMALY.name(),
                // 기댓값을 enum에서 되읽으면 유형↔제목이 뒤바뀌어도 통과하므로 문구를 직접 고정한다
                "빠른 이동 알림",
                "김소연 님이 차량 속도로 이동 중입니다.",
                Map.of(
                        "type", DetectionType.SPEED_ANOMALY.name(),
                        "wardKey", NotificationFixture.WARD_KEY,
                        "score", String.valueOf(LocationFixture.ANOMALY_SCORE),
                        "recordedAt", LocationFixture.DETECTED_AT_TEXT,
                        "latitude", String.valueOf(LocationFixture.LATITUDE),
                        "longitude", String.valueOf(LocationFixture.LONGITUDE)
                )
        );
    }

    @Test
    @DisplayName("그 탐지 유형의 알림을 끈 보호자에게만 보내지 않는다")
    void skips_only_caregiver_who_turned_off_the_type() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createSettingWithAnomalyToggles(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, true, true, true, true, false)));
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.TIME_ANOMALY, "{name} 님은 평소 이 시각에는 나가신 적이 거의 없습니다."));

        then(notificationSendManager).should().sendToCareParties(
                eq(List.of()),
                eq(List.of(NotificationFixture.MANAGER_KEY)),
                eq(DetectionType.TIME_ANOMALY.name()),
                anyString(), anyString(), any());
    }

    @Test
    @DisplayName("연결된 보호자가 없으면 보내지 않는다")
    void skips_when_no_caregiver() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of());

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.WANDERING, "{name} 님이 같은 곳을 맴돌고 계십니다."));

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
        then(memberReader).should(never()).findNameByMemberKey(anyString());
    }

    @Test
    @DisplayName("사건 형식 알림은 사건 식별자·알림 종류·사유 코드를 함께 담아 보낸다")
    void puts_event_identifiers_into_payload() {
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(LocationFixture.createAnomalyDetectedEvent(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.CATEGORY_ADDED, AnomalyReasonCode.DWELL_AFTER_VEHICLE,
                LocationFixture.anomalyMeasures(LocationFixture.DWELL_DURATION_MIN, null, null, null))));

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        then(notificationSendManager).should().sendToCareParties(
                any(), any(), anyString(), anyString(), anyString(), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("type", DetectionType.ABNORMAL_DWELLING.name())
                .containsEntry("wardKey", NotificationFixture.WARD_KEY)
                .containsEntry("recordedAt", LocationFixture.DETECTED_AT_TEXT)
                .containsEntry("eventId", LocationFixture.EVENT_ID)
                .containsEntry("alertKind", "CATEGORY_ADDED")
                .containsEntry("reasonCode", "DWELL_AFTER_VEHICLE");
    }

    @ParameterizedTest
    @EnumSource(value = AnomalyReasonCode.class, names = {"DWELL_KNOWN_LONG", "DWELL_SAFEZONE_LONG"})
    @DisplayName("아는 장소·안심존에 오래 머무는 알림은 장시간 정지와 구분되는 제목으로 보낸다")
    void sends_long_stay_title_for_known_place_and_safe_zone(AnomalyReasonCode reasonCode) {
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(LocationFixture.createAnomalyDetectedEvent(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.EVENT_START, reasonCode,
                LocationFixture.anomalyMeasures(LocationFixture.DWELL_DURATION_MIN, null, null, null))));

        assertThat(capturedTitle()).isEqualTo("오래 머무름 확인");
    }

    @Test
    @DisplayName("낯선 곳에서 움직임이 없는 알림은 장시간 정지 제목을 유지한다")
    void keeps_dwelling_title_for_unknown_place() {
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(LocationFixture.createAnomalyDetectedEvent(LocationFixture.createEventAnomalyAlert(
                DetectionType.ABNORMAL_DWELLING, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.DWELL_UNKNOWN,
                LocationFixture.anomalyMeasures(LocationFixture.DWELL_DURATION_MIN, null, null, null))));

        assertThat(capturedTitle()).isEqualTo("장시간 정지 알림");
    }

    @Test
    @DisplayName("차량 이동 재알림은 이동이 계속되고 있다는 제목으로 보낸다")
    void sends_continuing_vehicle_title_for_vehicle_remind() {
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(LocationFixture.createAnomalyDetectedEvent(LocationFixture.createEventAnomalyAlert(
                DetectionType.SPEED_ANOMALY, AnomalyAlertKind.REMIND, AnomalyReasonCode.VEHICLE,
                LocationFixture.anomalyMeasures(null, null, LocationFixture.VEHICLE_SPEED_KMH, null))));

        assertThat(capturedTitle()).isEqualTo("차량 이동 계속 중");
    }

    @Test
    @DisplayName("차량 이동 첫 알림은 빠른 이동 제목을 유지한다")
    void keeps_speed_title_for_first_vehicle_alert() {
        givenCaregiversAndWardName();

        anomalyNotificationListener.onAnomalyDetected(LocationFixture.createAnomalyDetectedEvent(LocationFixture.createEventAnomalyAlert(
                DetectionType.SPEED_ANOMALY, AnomalyAlertKind.EVENT_START, AnomalyReasonCode.VEHICLE,
                LocationFixture.anomalyMeasures(null, null, LocationFixture.VEHICLE_SPEED_KMH, null))));

        assertThat(capturedTitle()).isEqualTo("빠른 이동 알림");
    }

    private void givenCaregiversAndWardName() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.WARD_NAME);
    }

    private String capturedTitle() {
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        then(notificationSendManager).should().sendToCareParties(
                any(), any(), anyString(), titleCaptor.capture(), anyString(), any());
        return titleCaptor.getValue();
    }

    private String capturedBody() {
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        then(notificationSendManager).should().sendToCareParties(
                any(), any(), anyString(), anyString(), bodyCaptor.capture(), any());
        return bodyCaptor.getValue();
    }
}
