package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatteryThresholdNotificationListener 단위 테스트")
class BatteryThresholdNotificationListenerTest {

    private static final String EVENT_TYPE = "DEVICE_BATTERY_THRESHOLD";

    @InjectMocks
    private BatteryThresholdNotificationListener batteryThresholdNotificationListener;

    @Mock
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private NotificationSendManager notificationSendManager;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("보호자와 관계자를 역할별로 나눠 알림 발송을 위임한다")
    void onBatteryThresholdAlert_sends_to_care_parties_by_role() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.WARD_NAME);

        batteryThresholdNotificationListener.onBatteryThresholdAlert(
                LocationFixture.createBatteryThresholdAlertEvent());

        then(notificationSendManager).should(times(1)).sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                EVENT_TYPE,
                "배터리 잔량 알림",
                NotificationFixture.WARD_NAME + "님의 기기 배터리 잔량이 20%에 도달했어요.",
                Map.of(
                        "type", EVENT_TYPE,
                        "wardKey", NotificationFixture.WARD_KEY,
                        "thresholdPercent", "20"
                )
        );
    }

    @Test
    @DisplayName("수신자가 없으면 알림을 보내지 않는다")
    void onBatteryThresholdAlert_skips_when_no_caregiver() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of());

        batteryThresholdNotificationListener.onBatteryThresholdAlert(
                LocationFixture.createBatteryThresholdAlertEvent());

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
    }
}
