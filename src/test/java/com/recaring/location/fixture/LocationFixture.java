package com.recaring.location.fixture;

import com.recaring.location.dataaccess.entity.SafeZoneState;
import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.location.dataaccess.entity.AnomalyDetection;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.location.event.SafeZoneEnteredEvent;
import com.recaring.location.event.SafeZoneExitedEvent;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.DetectionType;
import com.recaring.location.vo.Gps;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.safezone.fixture.SafeZoneFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class LocationFixture {

    public static final String WARD_KEY = "ward-member-key-001";
    public static final String GUARDIAN_KEY = "guardian-member-key-001";
    public static final String MANAGER_KEY = "manager-member-key-001";
    public static final double LATITUDE = 37.5665;
    public static final double LONGITUDE = 126.9780;
    public static final Double ACCURACY = 15.0;
    public static final Integer BATTERY = 80;
    public static final int NOTIFIED_THRESHOLD = 20;
    public static final Double SPEED = 1.4;
    public static final String BATTERY_ALERT_STATE_KEY = "device:battery:" + WARD_KEY;
    public static final String LAST_NOTIFIED_THRESHOLD_FIELD = "lastNotifiedThreshold";
    public static final LocalDateTime MEASURED_AT = LocalDateTime.of(2026, 7, 27, 10, 15, 0);
    public static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 7, 27, 10, 15, 3);

    public static final LocalDateTime DETECTED_AT = LocalDateTime.of(2026, 7, 27, 10, 20, 5);
    public static final String DETECTED_AT_TEXT = "2026-07-27 10:20:05";
    public static final String DETECTED_AT_ISO_TEXT = "2026-07-27T10:20:05";
    public static final double ANOMALY_SCORE = 0.82;
    public static final String WANDERING_EVIDENCE = "{name} 님이 같은 곳을 맴돌고 계십니다.";
    public static final String SAFE_ZONE_KEY = SafeZoneFixture.SAFE_ZONE_KEY;
    public static final String SAFE_ZONE_NAME = "안심존 1";
    public static final String HOME_ZONE_KEY = "safe-zone-home";
    public static final String HOME_ZONE_NAME = "집";
    public static final String HOSPITAL_ZONE_KEY = "safe-zone-hospital";
    public static final String HOSPITAL_ZONE_NAME = "병원";

    public static Gps createGps() {
        return createGps(BATTERY);
    }

    public static Gps createGps(Integer battery) {
        return new Gps(LATITUDE, LONGITUDE, RECORDED_AT, ACCURACY, battery, SPEED, MEASURED_AT);
    }

    public static Gps createGpsWithAccuracy(Double accuracy) {
        return new Gps(LATITUDE, LONGITUDE, RECORDED_AT, accuracy, BATTERY, SPEED, MEASURED_AT);
    }

    public static Member createWard() {
        return Member.builder()
                .phone("01011112222")
                .name("보호대상자")
                .birth(LocalDate.of(1950, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.WARD)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createGuardian() {
        return Member.builder()
                .phone("01033334444")
                .name("보호자")
                .birth(LocalDate.of(1980, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createCoGuardian() {
        return Member.builder()
                .phone("01066665555")
                .name("공동보호자")
                .birth(LocalDate.of(1985, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createManager() {
        return Member.builder()
                .phone("01055556666")
                .name("관리자")
                .birth(LocalDate.of(1975, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static SafeZoneEnteredEvent createSafeZoneEnteredEvent() {
        return new SafeZoneEnteredEvent(WARD_KEY, SAFE_ZONE_KEY, SAFE_ZONE_NAME, DETECTED_AT);
    }

    public static SafeZoneExitedEvent createSafeZoneExitedEvent() {
        return new SafeZoneExitedEvent(WARD_KEY, SAFE_ZONE_KEY, SAFE_ZONE_NAME, DETECTED_AT);
    }

    public static BatteryThresholdAlertEvent createBatteryThresholdAlertEvent() {
        return new BatteryThresholdAlertEvent(WARD_KEY, NOTIFIED_THRESHOLD, DETECTED_AT);
    }

    public static AnomalyAlert createAnomalyAlert(DetectionType detectionType, String evidence) {
        return new AnomalyAlert(
                WARD_KEY, detectionType, ANOMALY_SCORE, DETECTED_AT, LATITUDE, LONGITUDE, evidence);
    }

    public static AnomalyDetection createAnomalyDetection(DetectionType detectionType, String evidence) {
        return createAnomalyDetection(detectionType, evidence, DETECTED_AT);
    }

    public static AnomalyDetection createAnomalyDetection(
            DetectionType detectionType, String evidence, LocalDateTime recordedAt) {
        return AnomalyDetection.builder()
                .wardMemberKey(WARD_KEY)
                .detectionType(detectionType)
                .score(ANOMALY_SCORE)
                .recordedAt(recordedAt)
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .evidence(evidence)
                .build();
    }

    public static AnomalyDetectedEvent createAnomalyDetectedEvent(DetectionType detectionType, String evidence) {
        return new AnomalyDetectedEvent(createAnomalyAlert(detectionType, evidence));
    }

    public static Map<String, String> createAnomalyStreamFields(DetectionType detectionType, String evidence) {
        Map<String, String> fields = new HashMap<>();
        fields.put("ward_member_key", WARD_KEY);
        fields.put("detection_type", detectionType.name());
        fields.put("score", String.valueOf(ANOMALY_SCORE));
        fields.put("recorded_at", DETECTED_AT_TEXT);
        fields.put("latitude", String.valueOf(LATITUDE));
        fields.put("longitude", String.valueOf(LONGITUDE));
        fields.put("evidence", evidence);
        return fields;
    }

    public static String gpsRequestBody() {
        return gpsRequestBody(LATITUDE, LONGITUDE);
    }

    public static String gpsRequestBody(double latitude, double longitude) {
        return "{\"latitude\": %s, \"longitude\": %s}".formatted(latitude, longitude);
    }

    public static String gpsCacheJson() {
        return "{\"latitude\":%s,\"longitude\":%s,\"recordedAt\":\"2024-01-01T00:00:00\"}"
                .formatted(LATITUDE, LONGITUDE);
    }

    public static SafeZoneState createSafeZoneState(String safeZoneKeysCsv) {
        return SafeZoneState.builder()
                .wardMemberKey(WARD_KEY)
                .safeZoneKeys(safeZoneKeysCsv)
                .build();
    }

    public static LocationSetting createLocationSetting(int collectionIntervalSeconds) {
        return LocationSetting.builder()
                .wardMemberKey(WARD_KEY)
                .collectionIntervalSeconds(collectionIntervalSeconds)
                .build();
    }
}
