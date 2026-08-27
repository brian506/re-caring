package com.recaring.notification.fixture;

import com.recaring.care.dataaccess.entity.CarePartyRole;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.vo.NotificationItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class NotificationFixture {

    public static final String WARD_KEY = LocationFixture.WARD_KEY;
    public static final String WARD_NAME = "김소연";
    public static final String GUARDIAN_KEY = LocationFixture.GUARDIAN_KEY;
    public static final String MANAGER_KEY = LocationFixture.MANAGER_KEY;
    public static final String OTHER_GUARDIAN_KEY = "other-guardian-member-key-001";
    public static final String GUARDIAN_FCM_TOKEN = "guardian-fcm-token-001";
    public static final String MANAGER_FCM_TOKEN = "manager-fcm-token-001";

    public static Member createWard() {
        return Member.builder()
                .phone("01011112222")
                .name("Ward")
                .birth(LocalDate.of(1950, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.WARD)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createGuardian() {
        return Member.builder()
                .phone("01033334444")
                .name("Guardian")
                .birth(LocalDate.of(1980, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createManager() {
        return Member.builder()
                .phone("01055556666")
                .name("Manager")
                .birth(LocalDate.of(1975, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createOtherGuardian() {
        return Member.builder()
                .phone("01077778888")
                .name("Other Guardian")
                .birth(LocalDate.of(1985, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static NotificationSetting createSetting(String wardKey) {
        return createSettingWithAnomalyToggles(wardKey, false, true, false, true, true);
    }

    public static NotificationSetting createSettingWithAnomalyToggles(
            String wardKey,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    ) {
        return NotificationSetting.builder()
                .wardMemberKey(wardKey)
                .safeZoneEntryEnabled(true)
                .safeZoneExitEnabled(false)
                .speedAnomalyEnabled(speedAnomalyEnabled)
                .wanderingAnomalyEnabled(wanderingAnomalyEnabled)
                .abnormalDwellingEnabled(abnormalDwellingEnabled)
                .routeDeviationEnabled(routeDeviationEnabled)
                .timeAnomalyEnabled(timeAnomalyEnabled)
                .emergencyCallEnabled(true)
                .lowBatteryEnabled(false)
                .batteryThresholdPercents("40,90")
                .build();
    }

    public static FcmDeviceToken guardianFcmDeviceToken(String token) {
        return FcmDeviceToken.builder()
                .memberKey(GUARDIAN_KEY)
                .careRole(CarePartyRole.GUARDIAN)
                .token(token)
                .platform(FcmDevicePlatform.ANDROID)
                .build();
    }

    public static FcmDeviceToken managerFcmDeviceToken(String token) {
        return FcmDeviceToken.builder()
                .memberKey(MANAGER_KEY)
                .careRole(CarePartyRole.MANAGER)
                .token(token)
                .platform(FcmDevicePlatform.IOS)
                .build();
    }

    public static Notification notification(String recipientMemberKey, String eventType, String title, String body) {
        return Notification.builder()
                .recipientMemberKey(recipientMemberKey)
                .eventType(eventType)
                .title(title)
                .body(body)
                .dataPayload(Map.of("type", eventType))
                .build();
    }

    public static NotificationItem notificationItem(String eventType, String title, String body) {
        return new NotificationItem(
                "notification-key-001",
                eventType,
                title,
                body,
                Map.of("type", eventType),
                LocalDateTime.of(2026, 7, 5, 9, 41)
        );
    }

}
