package com.recaring.location.fixture;

import com.recaring.location.dataaccess.entity.SafeZoneState;
import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.location.dataaccess.entity.AnomalyDetection;
import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.event.AnomalyDetectedEvent;
import com.recaring.location.event.BatteryThresholdAlertEvent;
import com.recaring.location.vo.BatteryAlertState;
import com.recaring.location.event.LocationSignalLostEvent;
import com.recaring.location.event.SafeZoneEnteredEvent;
import com.recaring.location.event.SafeZoneExitedEvent;
import com.recaring.location.vo.AnomalyAlert;
import com.recaring.location.vo.AnomalyAlertKind;
import com.recaring.location.vo.AnomalyEvent;
import com.recaring.location.vo.AnomalyMeasures;
import com.recaring.location.vo.AnomalyReasonCode;
import com.recaring.location.vo.DetectionType;
import com.recaring.location.vo.Gps;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.safezone.dataaccess.entity.SafeZoneType;
import com.recaring.safezone.fixture.SafeZoneFixture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class LocationFixture {

    public static final String WARD_KEY = "ward-member-key-001";
    public static final String GUARDIAN_KEY = "guardian-member-key-001";
    public static final String MANAGER_KEY = "manager-member-key-001";
    public static final String OTHER_GUARDIAN_KEY = "other-guardian-member-key-001";
    public static final double LATITUDE = 37.5665;
    public static final double LONGITUDE = 126.9780;
    public static final Double ACCURACY = 15.0;
    public static final Integer BATTERY = 80;
    public static final int NOTIFIED_THRESHOLD = 20;
    public static final Double SPEED = 1.4;
    public static final String BATTERY_ALERT_STATE_KEY = "device:battery:" + WARD_KEY;
    public static final LocalDateTime MEASURED_AT = LocalDateTime.of(2026, 7, 27, 10, 15, 0);
    public static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 7, 27, 10, 15, 3);
    public static final String MEASURED_AT_TEXT = "2026-07-27 10:15:00";
    public static final String RECORDED_AT_TEXT = "2026-07-27 10:15:03";
    public static final String OTHER_WARD_KEY = "ward-member-key-002";
    public static final String THIRD_WARD_KEY = "ward-member-key-003";

    public static final LocalDateTime DETECTED_AT = LocalDateTime.of(2026, 7, 27, 10, 20, 5);
    public static final String DETECTED_AT_TEXT = "2026-07-27 10:20:05";
    public static final String DETECTED_AT_ISO_TEXT = "2026-07-27T10:20:05";
    public static final double ANOMALY_SCORE = 0.82;
    public static final String WANDERING_EVIDENCE = "{name} 님이 같은 곳을 맴돌고 계십니다.";
    public static final LocalDateTime EVENT_STARTED_AT = LocalDateTime.of(2026, 7, 27, 10, 0, 0);
    public static final String EVENT_STARTED_AT_ISO_TEXT = "2026-07-27T10:00:00";
    public static final String EVENT_ID = WARD_KEY + ":" + EVENT_STARTED_AT_ISO_TEXT;
    public static final String PREVIOUS_EVENT_ID = WARD_KEY + ":2026-07-26T21:00:00";
    public static final String ANOMALY_ACK_KEY = "anomaly:ack:" + WARD_KEY;
    public static final String ANOMALY_CATEGORIES = "ABNORMAL_DWELLING,ROUTE_DEVIATION";
    public static final int DWELL_DURATION_MIN = 40;
    public static final int VEHICLE_SPEED_KMH = 92;
    public static final String SAFE_ZONE_KEY = SafeZoneFixture.SAFE_ZONE_KEY;
    public static final String SAFE_ZONE_NAME = "안심존 1";
    public static final String DANGER_ZONE_KEY = "danger-zone-key-001";
    public static final String DANGER_ZONE_NAME = SafeZoneFixture.DANGER_ZONE_NAME;
    public static final String HOME_ZONE_KEY = "safe-zone-home";
    public static final String HOME_ZONE_NAME = "집";
    public static final String HOSPITAL_ZONE_KEY = "safe-zone-hospital";
    public static final String HOSPITAL_ZONE_NAME = "병원";

    public static final LocalDateTime TRACK_START = LocalDateTime.of(2026, 7, 27, 10, 0, 0);
    public static final Double TRACK_ACCURACY = 10.0;
    public static final long TRACK_NETWORK_DELAY_SECONDS = 3;
    public static final Double INACCURATE_ACCURACY = 150.0;
    // 위도 1도 ≈ 111,195m (지구 평균 반지름 6,371,008.8m 기준)
    public static final double WALK_50M_LATITUDE_DELTA = 0.00045;
    public static final double JUMP_15KM_LATITUDE_DELTA = 0.135;
    public static final double JUMP_45KM_LATITUDE_DELTA = 0.405;
    public static final double SPIKE_1KM_LATITUDE_DELTA = 0.009;
    public static final int ZONE_RADIUS_300M = 300;

    public static final LocalDateTime ROUTE_EVENT_STARTED_AT = LocalDateTime.of(2026, 9, 24, 7, 4, 12);
    public static final LocalDateTime ROUTE_EVENT_ENDED_AT = LocalDateTime.of(2026, 9, 24, 10, 55, 0);
    public static final String ROUTE_EVENT_STARTED_AT_INSTANT_TEXT = "2026-09-23T22:04:12Z";
    public static final String ROUTE_EVENT_ENDED_AT_INSTANT_TEXT = "2026-09-24T01:55:00Z";
    public static final LocalDateTime OVERNIGHT_EVENT_STARTED_AT = LocalDateTime.of(2026, 9, 23, 23, 30, 0);
    public static final LocalDateTime OVERNIGHT_EVENT_ENDED_AT = LocalDateTime.of(2026, 9, 24, 0, 30, 0);

    public static Gps createGps() {
        return createGps(BATTERY);
    }

    public static Gps createGps(Integer battery) {
        return new Gps(LATITUDE, LONGITUDE, RECORDED_AT, ACCURACY, battery, SPEED, MEASURED_AT);
    }

    public static Gps createGpsWithoutDeviceMotion() {
        return new Gps(LATITUDE, LONGITUDE, RECORDED_AT, ACCURACY, BATTERY, null, null);
    }

    public static Gps createGpsReceivedAt(LocalDateTime recordedAt) {
        return new Gps(LATITUDE, LONGITUDE, recordedAt, ACCURACY, BATTERY, SPEED, MEASURED_AT);
    }

    public static Gps createGpsWithAccuracy(Double accuracy) {
        return new Gps(LATITUDE, LONGITUDE, RECORDED_AT, accuracy, BATTERY, SPEED, MEASURED_AT);
    }

    public static Gps createTrackPoint(double latitude, Double accuracy, LocalDateTime measuredAt) {
        return createTrackPoint(latitude, accuracy, measuredAt, measuredAt);
    }

    public static Gps createTrackPoint(double latitude, Double accuracy,
                                       LocalDateTime measuredAt, LocalDateTime recordedAt) {
        return new Gps(latitude, LONGITUDE, recordedAt, accuracy, BATTERY, SPEED, measuredAt);
    }

    public static GpsHistory createGpsHistory(String wardKey, double latitude, Double accuracy, LocalDateTime measuredAt) {
        return GpsHistory.builder()
                .wardMemberKey(wardKey)
                .latitude(latitude)
                .longitude(LONGITUDE)
                .recordedAt(measuredAt.plusSeconds(TRACK_NETWORK_DELAY_SECONDS))
                .accuracy(accuracy)
                .battery(BATTERY)
                .speed(SPEED)
                .measuredAt(measuredAt)
                .build();
    }

    public static GpsHistory createGpsHistoryRecordedAt(
            String wardKey, double latitude, Double accuracy, LocalDateTime recordedAt) {
        return GpsHistory.builder()
                .wardMemberKey(wardKey)
                .latitude(latitude)
                .longitude(LONGITUDE)
                .recordedAt(recordedAt)
                .accuracy(accuracy)
                .battery(BATTERY)
                .speed(SPEED)
                .measuredAt(recordedAt)
                .build();
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
        return new SafeZoneEnteredEvent(
                WARD_KEY, SAFE_ZONE_KEY, SAFE_ZONE_NAME, SafeZoneType.SAFE, LATITUDE, LONGITUDE, DETECTED_AT);
    }

    public static SafeZoneEnteredEvent createDangerZoneEnteredEvent() {
        return new SafeZoneEnteredEvent(
                WARD_KEY, DANGER_ZONE_KEY, DANGER_ZONE_NAME, SafeZoneType.DANGER, LATITUDE, LONGITUDE, DETECTED_AT);
    }

    public static SafeZoneExitedEvent createSafeZoneExitedEvent() {
        return new SafeZoneExitedEvent(
                WARD_KEY, SAFE_ZONE_KEY, SAFE_ZONE_NAME, SafeZoneType.SAFE, LATITUDE, LONGITUDE, DETECTED_AT);
    }

    public static SafeZoneExitedEvent createDangerZoneExitedEvent() {
        return new SafeZoneExitedEvent(
                WARD_KEY, DANGER_ZONE_KEY, DANGER_ZONE_NAME, SafeZoneType.DANGER, LATITUDE, LONGITUDE, DETECTED_AT);
    }

    public static LocationSignalLostEvent createLocationSignalLostEvent() {
        return new LocationSignalLostEvent(WARD_KEY, createGps());
    }

    public static BatteryAlertState batteryAlertState(int lastNotifiedThreshold) {
        return new BatteryAlertState(lastNotifiedThreshold);
    }

    public static BatteryThresholdAlertEvent createBatteryThresholdAlertEvent() {
        return createBatteryThresholdAlertEvent(List.of(GUARDIAN_KEY, MANAGER_KEY));
    }

    public static BatteryThresholdAlertEvent createBatteryThresholdAlertEvent(List<String> recipientKeys) {
        return new BatteryThresholdAlertEvent(WARD_KEY, NOTIFIED_THRESHOLD, DETECTED_AT, recipientKeys);
    }

    public static AnomalyAlert createAnomalyAlert(DetectionType detectionType, String evidence) {
        return new AnomalyAlert(
                WARD_KEY, detectionType, ANOMALY_SCORE, DETECTED_AT, LATITUDE, LONGITUDE, evidence,
                null, null, anomalyMeasures(null, null, null, null));
    }

    public static AnomalyAlert createEventAnomalyAlert(
            DetectionType detectionType, AnomalyAlertKind alertKind, AnomalyReasonCode reasonCode,
            AnomalyMeasures measures) {
        return createEventAnomalyAlert(detectionType, alertKind, reasonCode, measures, null);
    }

    public static AnomalyAlert createEventAnomalyAlert(
            DetectionType detectionType, AnomalyAlertKind alertKind, AnomalyReasonCode reasonCode,
            AnomalyMeasures measures, String evidence) {
        AnomalyEvent event = new AnomalyEvent(
                EVENT_ID,
                alertKind,
                EVENT_STARTED_AT,
                alertKind == AnomalyAlertKind.EVENT_END ? DETECTED_AT : null,
                detectionType.name(),
                alertKind == AnomalyAlertKind.REMIND ? 1 : null);
        return new AnomalyAlert(
                WARD_KEY, detectionType, ANOMALY_SCORE, DETECTED_AT, LATITUDE, LONGITUDE, evidence,
                event, reasonCode, measures);
    }

    public static AnomalyMeasures anomalyMeasures(Integer durationMin, Integer usualMin, Integer speedKmh, Integer hour) {
        return new AnomalyMeasures(durationMin, usualMin, speedKmh, hour);
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

    public static String anomalyEventId(String wardKey, LocalDateTime startedAt) {
        return wardKey + ":" + startedAt;
    }

    public static AnomalyDetection createEventStartDetection(String wardKey, LocalDateTime startedAt) {
        return createEventDetection(wardKey, AnomalyAlertKind.EVENT_START, startedAt, null, startedAt);
    }

    public static AnomalyDetection createEventEndDetection(String wardKey, LocalDateTime startedAt, LocalDateTime endedAt) {
        return createEventDetection(wardKey, AnomalyAlertKind.EVENT_END, startedAt, endedAt, endedAt);
    }

    private static AnomalyDetection createEventDetection(
            String wardKey, AnomalyAlertKind alertKind, LocalDateTime startedAt, LocalDateTime endedAt,
            LocalDateTime recordedAt) {
        return AnomalyDetection.builder()
                .wardMemberKey(wardKey)
                .detectionType(DetectionType.SPEED_ANOMALY)
                .score(ANOMALY_SCORE)
                .recordedAt(recordedAt)
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .alertKind(alertKind)
                .eventId(anomalyEventId(wardKey, startedAt))
                .eventStartedAt(startedAt)
                .eventEndedAt(endedAt)
                .categories(DetectionType.SPEED_ANOMALY.name())
                .reasonCode(AnomalyReasonCode.VEHICLE)
                .build();
    }

    public static AnomalyDetection createEventEndDetection(DetectionType detectionType, LocalDateTime endedAt) {
        return AnomalyDetection.builder()
                .wardMemberKey(WARD_KEY)
                .detectionType(detectionType)
                .score(ANOMALY_SCORE)
                .recordedAt(endedAt)
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .alertKind(AnomalyAlertKind.EVENT_END)
                .eventId(EVENT_ID)
                .eventStartedAt(EVENT_STARTED_AT)
                .eventEndedAt(endedAt)
                .categories(detectionType.name())
                .build();
    }

    public static AnomalyDetectedEvent createAnomalyDetectedEvent(DetectionType detectionType, String evidence) {
        return new AnomalyDetectedEvent(createAnomalyAlert(detectionType, evidence));
    }

    public static AnomalyDetectedEvent createAnomalyDetectedEvent(AnomalyAlert alert) {
        return new AnomalyDetectedEvent(alert);
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

    public static Map<String, String> createEventAnomalyStreamFields() {
        Map<String, String> fields = createAnomalyStreamFields(DetectionType.ABNORMAL_DWELLING, null);
        fields.remove("evidence");
        fields.put("alert_kind", AnomalyAlertKind.CATEGORY_ADDED.name());
        fields.put("event_id", EVENT_ID);
        fields.put("event_started_at", EVENT_STARTED_AT_ISO_TEXT);
        fields.put("categories", ANOMALY_CATEGORIES);
        fields.put("reason_code", AnomalyReasonCode.DWELL_AFTER_VEHICLE.name());
        fields.put("duration_min", String.valueOf(DWELL_DURATION_MIN));
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

    public static LocationSetting createLocationSetting(String caregiverKey, int collectionIntervalSeconds) {
        return createLocationSetting(WARD_KEY, caregiverKey, collectionIntervalSeconds);
    }

    public static LocationSetting createLocationSetting(String wardKey, String caregiverKey, int collectionIntervalSeconds) {
        return LocationSetting.builder()
                .wardMemberKey(wardKey)
                .caregiverMemberKey(caregiverKey)
                .collectionIntervalSeconds(collectionIntervalSeconds)
                .build();
    }
}
