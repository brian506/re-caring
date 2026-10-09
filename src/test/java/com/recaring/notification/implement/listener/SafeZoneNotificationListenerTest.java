package com.recaring.notification.implement.listener;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.notification.implement.NotificationSendManager;
import com.recaring.notification.implement.setting.NotificationSettingReader;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private SafeZoneNotificationListener safeZoneNotificationListener;

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
        safeZoneNotificationListener = new SafeZoneNotificationListener(
                careRelationshipReader,
                new NotificationSettingReader(notificationSettingRepository),
                notificationSendManager,
                memberReader);
    }

    @Test
    @DisplayName("안심존에 도착하면 누가 도착했는지와 위치 확인용 좌표·측정 시각을 담아 보낸다")
    void sends_entered_notification_with_ward_name() {
        givenCaregiversAndWardName();

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createSafeZoneEnteredEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_ENTERED",
                "안심존 진입 알림",
                "김소연님이 안심존 1에 도착했어요.",
                expectedPayload("SAFE_ZONE_ENTERED")
        );
    }

    @Test
    @DisplayName("안심존에서 벗어나면 누가 벗어났는지와 위치 확인용 좌표·측정 시각을 담아 보낸다")
    void sends_exited_notification_with_ward_name() {
        givenCaregiversAndWardName();

        safeZoneNotificationListener.onSafeZoneExited(LocationFixture.createSafeZoneExitedEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_EXITED",
                "안심존 이탈 알림",
                "김소연님이 안심존 1에서 벗어났어요.",
                expectedPayload("SAFE_ZONE_EXITED")
        );
    }

    @Test
    @DisplayName("진입 알림을 끈 보호자만 빼고 나머지에게 보낸다")
    void sends_entered_only_to_caregivers_who_kept_it_on() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createSafeZoneSetting(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, false, true)));
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.OTHER_GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY)).willReturn(NotificationFixture.WARD_NAME);

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createSafeZoneEnteredEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.OTHER_GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_ENTERED",
                "안심존 진입 알림",
                "김소연님이 안심존 1에 도착했어요.",
                expectedPayload("SAFE_ZONE_ENTERED")
        );
    }

    @Test
    @DisplayName("모든 수신자가 이탈 알림을 껐으면 보내지 않는다")
    void skips_exited_when_every_caregiver_turned_it_off() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        NotificationFixture.createSafeZoneSetting(NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, true, false),
                        NotificationFixture.createSafeZoneSetting(NotificationFixture.WARD_KEY, NotificationFixture.MANAGER_KEY, true, false)));
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));

        safeZoneNotificationListener.onSafeZoneExited(LocationFixture.createSafeZoneExitedEvent());

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("연결된 보호자가 없으면 보내지 않는다")
    void skips_when_no_caregiver() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY)).willReturn(List.of());

        safeZoneNotificationListener.onSafeZoneExited(LocationFixture.createSafeZoneExitedEvent());

        then(notificationSendManager).should(never())
                .sendToCareParties(any(), any(), anyString(), anyString(), anyString(), any());
        then(memberReader).should(never()).findNameByMemberKey(anyString());
    }

    @Test
    @DisplayName("보호자는 보호자 수신자로, 관계자는 관계자 수신자로 분류된다")
    void classifies_guardians_and_managers_into_separate_recipients() {
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.OTHER_GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.WARD_NAME);

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createSafeZoneEnteredEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY, NotificationFixture.OTHER_GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "SAFE_ZONE_ENTERED",
                "안심존 진입 알림",
                "김소연님이 안심존 1에 도착했어요.",
                expectedPayload("SAFE_ZONE_ENTERED")
        );
    }

    @Test
    @DisplayName("위험존에 들어가면 위험존 진입 알림으로 보낸다")
    void sends_danger_zone_entered_notification() {
        givenCaregiversAndWardName();

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createDangerZoneEnteredEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "DANGER_ZONE_ENTERED",
                "위험존 진입 알림",
                "김소연님이 위험존 공사장에 들어갔어요.",
                expectedDangerPayload("DANGER_ZONE_ENTERED")
        );
    }

    @Test
    @DisplayName("위험존에서 벗어나면 위험존 이탈 알림으로 보낸다")
    void sends_danger_zone_exited_notification() {
        givenCaregiversAndWardName();

        safeZoneNotificationListener.onSafeZoneExited(LocationFixture.createDangerZoneExitedEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "DANGER_ZONE_EXITED",
                "위험존 이탈 알림",
                "김소연님이 위험존 공사장에서 벗어났어요.",
                expectedDangerPayload("DANGER_ZONE_EXITED")
        );
    }

    @Test
    @DisplayName("안심존 진입 알림을 끈 보호자도 위험존 진입 알림은 받는다")
    void sends_danger_zone_entered_even_if_safe_zone_entry_is_off() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createSafeZoneSetting(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, false, false)));
        givenCaregiversAndWardName();

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createDangerZoneEnteredEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "DANGER_ZONE_ENTERED",
                "위험존 진입 알림",
                "김소연님이 위험존 공사장에 들어갔어요.",
                expectedDangerPayload("DANGER_ZONE_ENTERED")
        );
    }

    @Test
    @DisplayName("위험존 진입 알림을 끈 보호자만 빼고 나머지에게 보낸다")
    void sends_danger_zone_entered_only_to_caregivers_who_kept_it_on() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createDangerZoneSetting(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, false, true)));
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.OTHER_GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY)).willReturn(NotificationFixture.WARD_NAME);

        safeZoneNotificationListener.onSafeZoneEntered(LocationFixture.createDangerZoneEnteredEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.OTHER_GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "DANGER_ZONE_ENTERED",
                "위험존 진입 알림",
                "김소연님이 위험존 공사장에 들어갔어요.",
                expectedDangerPayload("DANGER_ZONE_ENTERED")
        );
    }

    @Test
    @DisplayName("위험존 이탈 알림을 끈 보호자만 빼고 나머지에게 보낸다")
    void sends_danger_zone_exited_only_to_caregivers_who_kept_it_on() {
        given(notificationSettingRepository.findAllByWardMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(List.of(NotificationFixture.createDangerZoneSetting(
                        NotificationFixture.WARD_KEY, NotificationFixture.GUARDIAN_KEY, true, false)));
        given(careRelationshipReader.findCaregiverInfos(NotificationFixture.WARD_KEY))
                .willReturn(List.of(
                        CareFixture.createCaregiverInfo(NotificationFixture.GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.OTHER_GUARDIAN_KEY, CareRole.GUARDIAN),
                        CareFixture.createCaregiverInfo(NotificationFixture.MANAGER_KEY, CareRole.MANAGER)));
        given(memberReader.findNameByMemberKey(NotificationFixture.WARD_KEY)).willReturn(NotificationFixture.WARD_NAME);

        safeZoneNotificationListener.onSafeZoneExited(LocationFixture.createDangerZoneExitedEvent());

        then(notificationSendManager).should().sendToCareParties(
                List.of(NotificationFixture.OTHER_GUARDIAN_KEY),
                List.of(NotificationFixture.MANAGER_KEY),
                "DANGER_ZONE_EXITED",
                "위험존 이탈 알림",
                "김소연님이 위험존 공사장에서 벗어났어요.",
                expectedDangerPayload("DANGER_ZONE_EXITED")
        );
    }

    private Map<String, String> expectedDangerPayload(String type) {
        return Map.of(
                "type", type,
                "wardKey", NotificationFixture.WARD_KEY,
                "safeZoneKey", LocationFixture.DANGER_ZONE_KEY,
                "recordedAt", LocationFixture.DETECTED_AT_TEXT,
                "latitude", String.valueOf(LocationFixture.LATITUDE),
                "longitude", String.valueOf(LocationFixture.LONGITUDE)
        );
    }

    private Map<String, String> expectedPayload(String type) {
        return Map.of(
                "type", type,
                "wardKey", NotificationFixture.WARD_KEY,
                "safeZoneKey", LocationFixture.SAFE_ZONE_KEY,
                "recordedAt", LocationFixture.DETECTED_AT_TEXT,
                "latitude", String.valueOf(LocationFixture.LATITUDE),
                "longitude", String.valueOf(LocationFixture.LONGITUDE)
        );
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
