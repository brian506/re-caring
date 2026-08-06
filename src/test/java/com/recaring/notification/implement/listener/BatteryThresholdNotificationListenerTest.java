package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    private static final int THRESHOLD_PERCENT = 20;
    private static final String EVENT_TYPE = "DEVICE_BATTERY_THRESHOLD";
    private static final LocalDateTime DETECTED_AT = LocalDateTime.of(2026, 7, 27, 10, 15);

    @InjectMocks
    private BatteryThresholdNotificationListener batteryThresholdNotificationListener;

    @Mock
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private NotificationSendManager notificationSendManager;

    @Test
    @DisplayName("보호자와 관계자를 역할별로 나눠 알림 발송을 위임한다")
    void onBatteryThresholdAlert_sends_to_care_parties_by_role() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        caregiver(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        caregiver(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));

        batteryThresholdNotificationListener.onBatteryThresholdAlert(alertEvent());

        then(notificationSendManager).should(times(1)).sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                EVENT_TYPE,
                "배터리 잔량 알림",
                "기기 배터리 잔량이 20%에 도달했어요.",
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

        batteryThresholdNotificationListener.onBatteryThresholdAlert(alertEvent());

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
    }

    private CaregiverInfo caregiver(String memberKey, CareRole careRole) {
        return new CaregiverInfo(memberKey, "name", "01011112222", careRole);
    }

    private BatteryThresholdAlertEvent alertEvent() {
        return new BatteryThresholdAlertEvent(NotificationFixture.WARD_KEY, THRESHOLD_PERCENT, DETECTED_AT);
    }
}
