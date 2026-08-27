package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.DetectionType;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("이상탐지 알림 리스너 단위 테스트")
class AnomalyNotificationListenerTest {

    private static final int MAX_BODY_LENGTH = 1000;

    @InjectMocks
    private AnomalyNotificationListener anomalyNotificationListener;

    @Mock
    private CareRelationshipReader careRelationshipReader;
    @Mock
    private NotificationSettingReader notificationSettingReader;
    @Mock
    private NotificationSendManager notificationSendManager;
    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("탐지 근거 문구의 이름 자리에 대상자 이름을 채워 보낸다")
    void fills_ward_name_into_evidence() {
        givenEnabledWithCaregivers(DetectionType.WANDERING);

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.WANDERING, "{name} 님이 같은 곳을 맴돌고 계십니다."));

        assertThat(capturedBody()).isEqualTo("김소연 님이 같은 곳을 맴돌고 계십니다.");
    }

    @Test
    @DisplayName("보호자와 관계자를 역할별로 나눠 알림 발송을 위임한다")
    void splits_recipients_by_care_role() {
        givenEnabledWithCaregivers(DetectionType.SPEED_ANOMALY);

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
                        "detectedAt", LocationFixture.DETECTED_AT_TEXT,
                        "latitude", String.valueOf(LocationFixture.LATITUDE),
                        "longitude", String.valueOf(LocationFixture.LONGITUDE)
                )
        );
    }

    @Test
    @DisplayName("해당 탐지 유형의 알림을 꺼두었으면 보내지 않는다")
    void skips_when_type_toggle_is_off() {
        given(notificationSettingReader.isAnomalyEnabled(
                NotificationFixture.WARD_KEY, DetectionType.TIME_ANOMALY)).willReturn(false);

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.TIME_ANOMALY, "{name} 님은 평소 이 시각에는 나가신 적이 거의 없습니다."));

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("연결된 보호자가 없으면 보내지 않는다")
    void skips_when_no_caregiver() {
        given(notificationSettingReader.isAnomalyEnabled(
                NotificationFixture.WARD_KEY, DetectionType.WANDERING)).willReturn(true);
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of());

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.WANDERING, "{name} 님이 같은 곳을 맴돌고 계십니다."));

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
        then(memberReader).should(never()).findNameByMemberKey(anyString());
    }

    @Test
    @DisplayName("탐지 근거 문구가 비어 있어도 본문 없는 알림을 보내지 않는다")
    void falls_back_when_evidence_is_blank() {
        givenEnabledWithCaregivers(DetectionType.ABNORMAL_DWELLING);

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.ABNORMAL_DWELLING, ""));

        assertThat(capturedBody()).isEqualTo("김소연님에게 이상 징후가 감지되었어요.");
    }

    @Test
    @DisplayName("이름을 채운 뒤 본문이 정확히 1000자면 자르지 않는다")
    void keeps_body_at_the_length_limit() {
        givenEnabledWithCaregivers(DetectionType.ROUTE_DEVIATION);

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.ROUTE_DEVIATION, "{name}" + "가".repeat(MAX_BODY_LENGTH - 3)));

        assertThat(capturedBody()).hasSize(MAX_BODY_LENGTH);
        assertThat(capturedBody()).startsWith(NotificationFixture.WARD_NAME);
        assertThat(capturedBody()).endsWith("가");
    }

    @Test
    @DisplayName("이름을 채운 뒤 본문이 저장 한도를 넘으면 1000자로 자른다")
    void truncates_body_beyond_the_length_limit() {
        givenEnabledWithCaregivers(DetectionType.ROUTE_DEVIATION);

        anomalyNotificationListener.onAnomalyDetected(
                LocationFixture.createAnomalyDetectedEvent(DetectionType.ROUTE_DEVIATION, "{name}" + "가".repeat(MAX_BODY_LENGTH)));

        assertThat(capturedBody()).hasSize(MAX_BODY_LENGTH);
    }

    private void givenEnabledWithCaregivers(DetectionType detectionType) {
        given(notificationSettingReader.isAnomalyEnabled(NotificationFixture.WARD_KEY, detectionType))
                .willReturn(true);
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.WARD_NAME);
    }

    private String capturedBody() {
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        then(notificationSendManager).should().sendToCareParties(
                any(), any(), anyString(), anyString(), bodyCaptor.capture(), any());
        return bodyCaptor.getValue();
    }
}
