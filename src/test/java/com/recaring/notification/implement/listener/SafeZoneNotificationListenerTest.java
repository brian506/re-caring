package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("안심존 알림 리스너 단위 테스트")
class SafeZoneNotificationListenerTest {

    @InjectMocks
    private SafeZoneNotificationListener safeZoneNotificationListener;

    @Mock
    private CareRelationshipReader careRelationshipReader;
    @Mock
    private NotificationSettingReader notificationSettingReader;
    @Mock
    private NotificationSendManager notificationSendManager;
    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("안심존에 도착하면 누가 도착했는지 문구에 담아 보낸다")
    void sends_entered_notification_with_ward_name() {
        given(notificationSettingReader.isSafeZoneEntryEnabled(NotificationFixture.WARD_KEY)).willReturn(true);
        givenCaregiversAndWardName();

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createSafeZoneEnteredEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_ENTERED",
                "안심존 진입 알림",
                "김소연님이 안심존 1에 도착했어요.",
                Map.of(
                        "type", "SAFE_ZONE_ENTERED",
                        "wardKey", NotificationFixture.WARD_KEY,
                        "safeZoneKey", LocationFixture.SAFE_ZONE_KEY
                )
        );
    }

    @Test
    @DisplayName("안심존에서 벗어나면 누가 벗어났는지 문구에 담아 보낸다")
    void sends_exited_notification_with_ward_name() {
        given(notificationSettingReader.isSafeZoneExitEnabled(NotificationFixture.WARD_KEY)).willReturn(true);
        givenCaregiversAndWardName();

        safeZoneNotificationListener.onSafeZoneExited(LocationFixture.createSafeZoneExitedEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_EXITED",
                "안심존 이탈 알림",
                "김소연님이 안심존 1에서 벗어났어요.",
                Map.of(
                        "type", "SAFE_ZONE_EXITED",
                        "wardKey", NotificationFixture.WARD_KEY,
                        "safeZoneKey", LocationFixture.SAFE_ZONE_KEY
                )
        );
    }

    @Test
    @DisplayName("진입 알림을 꺼두었으면 보내지 않는다")
    void skips_entered_when_toggle_is_off() {
        given(notificationSettingReader.isSafeZoneEntryEnabled(NotificationFixture.WARD_KEY)).willReturn(false);

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createSafeZoneEnteredEvent());

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("연결된 보호자가 없으면 보내지 않는다")
    void skips_when_no_caregiver() {
        given(notificationSettingReader.isSafeZoneExitEnabled(NotificationFixture.WARD_KEY)).willReturn(true);
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY)).willReturn(List.of());

        safeZoneNotificationListener.onSafeZoneExited(LocationFixture.createSafeZoneExitedEvent());

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
        then(memberReader).should(never()).findNameByMemberKey(anyString());
    }

    private void givenCaregiversAndWardName() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.WARD_NAME);
    }
}
