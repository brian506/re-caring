package com.recaring.notification.fixture;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.notification.business.command.NotificationSendCommand;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.vo.AnomalySensitivity;
import com.recaring.notification.vo.NotificationItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class NotificationFixture {

    public static final String WARD_KEY = "ward-member-key-001";
    public static final String GUARDIAN_KEY = "guardian-member-key-001";
    public static final String MANAGER_KEY = "manager-member-key-001";
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
        // A manager in a care relationship still uses the GUARDIAN member role.
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
        return NotificationSetting.builder()
                .wardMemberKey(wardKey)
                .safeZoneEntryEnabled(true)
                .safeZoneExitEnabled(false)
                .routeDeviationEnabled(true)
                .speedAnomalyEnabled(false)
                .wanderingAnomalyEnabled(true)
                .anomalySensitivity(AnomalySensitivity.HIGH)
                .emergencyCallEnabled(true)
                .lowBatteryEnabled(false)
                .batteryThresholdPercent(40)
                .build();
    }

    public static FcmDeviceToken guardianFcmDeviceToken(String token) {
        return FcmDeviceToken.builder()
                .memberKey(GUARDIAN_KEY)
                .careRole(CareRole.GUARDIAN)
                .token(token)
                .platform(FcmDevicePlatform.ANDROID)
                .build();
    }

    public static FcmDeviceToken managerFcmDeviceToken(String token) {
        return FcmDeviceToken.builder()
                .memberKey(MANAGER_KEY)
                .careRole(CareRole.MANAGER)
                .token(token)
                .platform(FcmDevicePlatform.IOS)
                .build();
    }

    public static NotificationSendCommand sendCommand() {
        return new NotificationSendCommand(
                List.of(GUARDIAN_KEY),
                List.of(MANAGER_KEY),
                "title",
                "body",
                Map.of("wardKey", WARD_KEY),
                "SAFE_ZONE_EXIT"
        );
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
