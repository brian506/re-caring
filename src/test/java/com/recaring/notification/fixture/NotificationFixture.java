package com.recaring.notification.fixture;

import com.recaring.notification.vo.FcmDeviceTokenInfo;
import com.recaring.care.vo.CarePartyRole;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.DetectionType;
import com.recaring.member.vo.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.vo.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FeedbackAccuracy;
import com.recaring.notification.dataaccess.entity.FeedbackReason;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.notification.vo.BatteryNotificationSetting;
import com.recaring.notification.vo.BatteryThresholds;
import com.recaring.notification.vo.NotificationFeedbackAnswer;
import com.recaring.notification.vo.NotificationItem;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class NotificationFixture {

    public static final String WARD_KEY = LocationFixture.WARD_KEY;
    public static final String WARD_NAME = "김소연";
    public static final String GUARDIAN_KEY = LocationFixture.GUARDIAN_KEY;
    public static final String SECOND_GUARDIAN_KEY = LocationFixture.SECOND_GUARDIAN_KEY;
    public static final String OTHER_GUARDIAN_KEY = LocationFixture.OTHER_GUARDIAN_KEY;
    public static final String GUARDIAN_FCM_TOKEN = "guardian-fcm-token-001";
    public static final String SECOND_GUARDIAN_FCM_TOKEN = "second-guardian-fcm-token-001";
    public static final String BATTERY_LOW_EVENT_TYPE = "BATTERY_LOW";
    public static final String BATTERY_LOW_TITLE = "배터리 부족";
    public static final String BATTERY_LOW_BODY = "배터리가 부족합니다. 잔량은 40% 입니다.";
    public static final DetectionType ANOMALY_TYPE = DetectionType.ROUTE_DEVIATION;
    public static final String ANOMALY_EVENT_TYPE = ANOMALY_TYPE.name();
    public static final String ANOMALY_TITLE = "낯선 장소 알림";
    public static final String ANOMALY_BODY = "김소연님이 지정된 경로에서 이탈했습니다.";
    public static final String ANOMALY_EVIDENCE = "{name} 님이 지정된 경로에서 이탈했습니다.";
    public static final LocalDateTime ANOMALY_RECORDED_AT = LocationFixture.DETECTED_AT;
    public static final String SAFE_ZONE_EVENT_TYPE = "SAFE_ZONE_EXITED";
    public static final String SAFE_ZONE_TITLE = "안심존 이탈 알림";
    public static final String SAFE_ZONE_BODY = "김소연님이 안심존 1에서 벗어났어요.";
    public static final String DANGER_ZONE_EVENT_TYPE = "DANGER_ZONE_ENTERED";
    public static final String DANGER_ZONE_TITLE = "위험존 진입 알림";
    public static final String DANGER_ZONE_BODY = "김소연님이 위험존 공사장에 들어갔어요.";
    public static final Long FEEDBACK_NOTIFICATION_ID = 30L;
    public static final String FEEDBACK_COMMENT = "집에 계셨어요";

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

    public static Member createSecondGuardian() {
        return Member.builder()
                .phone("01055556666")
                .name("Second Guardian")
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

    public static NotificationSetting createSetting(String wardKey, String caregiverKey) {
        return createSettingWithAnomalyToggles(wardKey, caregiverKey, false, true, false, true, true);
    }

    public static NotificationSetting createSettingWithAnomalyToggles(
            String wardKey,
            String caregiverKey,
            boolean speedAnomalyEnabled,
            boolean wanderingAnomalyEnabled,
            boolean abnormalDwellingEnabled,
            boolean routeDeviationEnabled,
            boolean timeAnomalyEnabled
    ) {
        return NotificationSetting.builder()
                .wardMemberKey(wardKey)
                .caregiverMemberKey(caregiverKey)
                .safeZoneEntryEnabled(true)
                .safeZoneExitEnabled(false)
                .dangerZoneEntryEnabled(true)
                .dangerZoneExitEnabled(false)
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

    public static NotificationSetting createSafeZoneSetting(
            String wardKey,
            String caregiverKey,
            boolean entryEnabled,
            boolean exitEnabled
    ) {
        NotificationSetting setting = NotificationSetting.defaultFor(wardKey, caregiverKey);
        setting.updateSafeZone(entryEnabled, exitEnabled);
        return setting;
    }

    public static NotificationSetting createDangerZoneSetting(
            String wardKey,
            String caregiverKey,
            boolean entryEnabled,
            boolean exitEnabled
    ) {
        NotificationSetting setting = NotificationSetting.defaultFor(wardKey, caregiverKey);
        setting.updateDangerZone(entryEnabled, exitEnabled);
        return setting;
    }

    public static NotificationSetting createBatterySetting(
            String wardKey,
            String caregiverKey,
            boolean lowBatteryEnabled,
            String batteryThresholdPercents
    ) {
        NotificationSetting setting = NotificationSetting.defaultFor(wardKey, caregiverKey);
        setting.updateBattery(lowBatteryEnabled, BatteryThresholds.parse(batteryThresholdPercents));
        return setting;
    }

    public static BatteryNotificationSetting batteryNotificationSetting(boolean enabled, Integer... percents) {
        return new BatteryNotificationSetting(enabled, BatteryThresholds.ofPercents(List.of(percents)));
    }

    public static FcmDeviceToken guardianFcmDeviceToken(String token) {
        return FcmDeviceToken.builder()
                .memberKey(GUARDIAN_KEY)
                .careRole(CarePartyRole.GUARDIAN)
                .token(token)
                .platform(FcmDevicePlatform.ANDROID)
                .build();
    }

    public static FcmDeviceToken secondGuardianFcmDeviceToken(String token) {
        return FcmDeviceToken.builder()
                .memberKey(SECOND_GUARDIAN_KEY)
                .careRole(CarePartyRole.GUARDIAN)
                .token(token)
                .platform(FcmDevicePlatform.IOS)
                .build();
    }

    public static FcmDeviceTokenInfo guardianFcmDeviceTokenInfo(String token) {
        return FcmDeviceTokenInfo.from(guardianFcmDeviceToken(token));
    }

    public static FcmDeviceTokenInfo secondGuardianFcmDeviceTokenInfo(String token) {
        return FcmDeviceTokenInfo.from(secondGuardianFcmDeviceToken(token));
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

    public static Notification batteryLowNotification(String recipientMemberKey) {
        return notification(recipientMemberKey, BATTERY_LOW_EVENT_TYPE, BATTERY_LOW_TITLE, BATTERY_LOW_BODY);
    }

    public static Notification notificationWithId(Long id, String recipientMemberKey) {
        Notification notification = batteryLowNotification(recipientMemberKey);
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    public static NotificationItem notificationItem(Long id) {
        return new NotificationItem(
                id,
                "notification-key-" + id,
                BATTERY_LOW_EVENT_TYPE,
                BATTERY_LOW_TITLE,
                BATTERY_LOW_BODY,
                Map.of("type", BATTERY_LOW_EVENT_TYPE),
                LocalDateTime.of(2026, 7, 5, 9, 41),
                false,
                false
        );
    }

    public static Map<String, String> anomalyDataPayload() {
        return Map.of(
                "type", ANOMALY_EVENT_TYPE,
                "wardKey", WARD_KEY,
                "score", String.valueOf(LocationFixture.ANOMALY_SCORE),
                "recordedAt", LocationFixture.DETECTED_AT_TEXT,
                "latitude", String.valueOf(LocationFixture.LATITUDE),
                "longitude", String.valueOf(LocationFixture.LONGITUDE)
        );
    }

    public static Notification anomalyNotification(String recipientMemberKey) {
        return anomalyNotification(recipientMemberKey, anomalyDataPayload());
    }

    public static Notification anomalyNotification(String recipientMemberKey, Map<String, String> dataPayload) {
        return Notification.builder()
                .recipientMemberKey(recipientMemberKey)
                .eventType(ANOMALY_EVENT_TYPE)
                .title(ANOMALY_TITLE)
                .body(ANOMALY_BODY)
                .dataPayload(dataPayload)
                .build();
    }

    public static Notification anomalyNotificationWithId(Long id, String recipientMemberKey) {
        return withId(id, anomalyNotification(recipientMemberKey));
    }

    public static Notification anomalyNotificationWithId(Long id, String recipientMemberKey, Map<String, String> dataPayload) {
        return withId(id, anomalyNotification(recipientMemberKey, dataPayload));
    }

    public static Map<String, String> safeZoneDataPayload() {
        return Map.of(
                "type", SAFE_ZONE_EVENT_TYPE,
                "wardKey", WARD_KEY,
                "safeZoneKey", LocationFixture.SAFE_ZONE_KEY,
                "recordedAt", LocationFixture.DETECTED_AT_TEXT,
                "latitude", String.valueOf(LocationFixture.LATITUDE),
                "longitude", String.valueOf(LocationFixture.LONGITUDE)
        );
    }

    public static Notification safeZoneNotification(String recipientMemberKey) {
        return Notification.builder()
                .recipientMemberKey(recipientMemberKey)
                .eventType(SAFE_ZONE_EVENT_TYPE)
                .title(SAFE_ZONE_TITLE)
                .body(SAFE_ZONE_BODY)
                .dataPayload(safeZoneDataPayload())
                .build();
    }

    public static Notification dangerZoneNotification(String recipientMemberKey) {
        return Notification.builder()
                .recipientMemberKey(recipientMemberKey)
                .eventType(DANGER_ZONE_EVENT_TYPE)
                .title(DANGER_ZONE_TITLE)
                .body(DANGER_ZONE_BODY)
                .dataPayload(Map.of(
                        "type", DANGER_ZONE_EVENT_TYPE,
                        "wardKey", WARD_KEY,
                        "safeZoneKey", LocationFixture.DANGER_ZONE_KEY,
                        "recordedAt", LocationFixture.DETECTED_AT_TEXT,
                        "latitude", String.valueOf(LocationFixture.LATITUDE),
                        "longitude", String.valueOf(LocationFixture.LONGITUDE)))
                .build();
    }

    public static Notification safeZoneNotificationWithId(Long id, String recipientMemberKey) {
        return withId(id, safeZoneNotification(recipientMemberKey));
    }

    private static Notification withId(Long id, Notification notification) {
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    public static NotificationFeedbackAnswer accurateFeedbackAnswer() {
        return new NotificationFeedbackAnswer(FeedbackAccuracy.ACCURATE, null, null);
    }

    public static NotificationFeedbackAnswer inaccurateFeedbackAnswer(FeedbackReason reason, String comment) {
        return new NotificationFeedbackAnswer(FeedbackAccuracy.INACCURATE, reason, comment);
    }
}
