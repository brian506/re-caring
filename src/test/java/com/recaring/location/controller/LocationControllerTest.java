package com.recaring.location.controller;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.care.fixture.CareFixture;
import com.recaring.device.fixture.DeviceFixture;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.location.dataaccess.repository.AnomalyDetectionRepository;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.location.vo.AnomalyAlertKind;
import com.recaring.location.vo.AnomalyReasonCode;
import com.recaring.location.vo.DetectionType;
import org.springframework.transaction.support.TransactionTemplate;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocationController HTTP 통합 테스트")
class LocationControllerTest extends AbstractIntegrationTest {

    private static final double INVALID_LATITUDE = 200.0;

    @Autowired private MemberRepository memberRepository;
    @Autowired private LocalAuthRepository localAuthRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;
    @Autowired private WardDeviceTokenRepository wardDeviceTokenRepository;
    @Autowired private GpsHistoryRepository gpsHistoryRepository;
    @Autowired private AnomalyDetectionRepository anomalyDetectionRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private Member ward;
    private Member guardian;
    private Member manager;
    private String guardianJwtToken;
    private String managerJwtToken;
    private String wardJwtToken;
    private String wardDeviceToken;

    @BeforeEach
    void setUp() {
        ward = memberRepository.save(LocationFixture.createWard());
        guardian = memberRepository.save(LocationFixture.createGuardian());
        manager = memberRepository.save(LocationFixture.createManager());

        String encoded = passwordEncoder.encode(AuthFixture.RAW_PASSWORD);
        localAuthRepository.save(AuthFixture.createLocalAuth(ward.getMemberKey(), encoded));
        localAuthRepository.save(AuthFixture.createLocalAuth(guardian.getMemberKey(), encoded));
        localAuthRepository.save(AuthFixture.createLocalAuth(manager.getMemberKey(), encoded));

        guardianJwtToken = extractAccessToken(guardian.getPhone(), AuthFixture.RAW_PASSWORD);
        managerJwtToken = extractAccessToken(manager.getPhone(), AuthFixture.RAW_PASSWORD);
        wardJwtToken = extractAccessToken(ward.getPhone(), AuthFixture.RAW_PASSWORD);

        WardDeviceToken deviceToken = DeviceFixture.createDeviceToken(ward.getMemberKey());
        wardDeviceTokenRepository.save(deviceToken);
        wardDeviceToken = deviceToken.getToken();

        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));
    }

    @AfterEach
    void tearDown() {
        careRelationshipRepository.deleteAllInBatch();
        gpsHistoryRepository.deleteAllInBatch();
        anomalyDetectionRepository.deleteAllInBatch();
        wardDeviceTokenRepository.deleteAllInBatch();
        localAuthRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    // ── GPS 수신 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/location/gps - 유효한 Device Token으로 GPS를 전송하면 200이 반환된다")
    void receiveGps_returns_200_with_valid_device_token() {
        client.post()
                .uri("/api/v1/location/gps")
                .header(HttpHeaders.AUTHORIZATION, "Device " + wardDeviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(LocationFixture.gpsRequestBody())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /api/v1/location/gps - Device Token 없이 요청하면 401이 반환된다")
    void receiveGps_returns_401_without_device_token() {
        client.post()
                .uri("/api/v1/location/gps")
                .contentType(MediaType.APPLICATION_JSON)
                .body(LocationFixture.gpsRequestBody())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/location/gps - 유효하지 않은 Device Token이면 401이 반환된다")
    void receiveGps_returns_401_with_invalid_device_token() {
        client.post()
                .uri("/api/v1/location/gps")
                .header(HttpHeaders.AUTHORIZATION, "Device invalid-token-xyz")
                .contentType(MediaType.APPLICATION_JSON)
                .body(LocationFixture.gpsRequestBody())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/location/gps - 위도 범위(-90~90)를 벗어나면 400이 반환된다")
    void receiveGps_returns_400_for_invalid_latitude() {
        client.post()
                .uri("/api/v1/location/gps")
                .header(HttpHeaders.AUTHORIZATION, "Device " + wardDeviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(LocationFixture.gpsRequestBody(INVALID_LATITUDE, LocationFixture.LONGITUDE))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── 이동 경로 히스토리 조회 ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/location/history/{wardKey} - GUARDIAN이 조회하면 전송된 좌표가 그대로 반환된다")
    void getHistory_returns_recorded_coordinates_for_guardian() {
        sendGps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE).expectStatus().isOk();

        getHistoryAsToday(guardianJwtToken)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].latitude").isEqualTo(LocationFixture.LATITUDE)
                .jsonPath("$.data[0].longitude").isEqualTo(LocationFixture.LONGITUDE);
    }

    @Test
    @DisplayName("GET /api/v1/location/history/{wardKey} - 정확도 150m 좌표와 튄 좌표는 응답에서 빠지고 DB에는 원본이 모두 남는다")
    void getHistory_excludes_inaccurate_and_jumped_points_but_keeps_raw_rows() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocalDate.now().atTime(LocationFixture.TRACK_START.toLocalTime());
        double walked = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA;
        double walkedTwice = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA * 2;
        gpsHistoryRepository.saveAll(List.of(
                LocationFixture.createGpsHistory(wardKey, LocationFixture.LATITUDE, LocationFixture.TRACK_ACCURACY, start),
                LocationFixture.createGpsHistory(wardKey, walked, LocationFixture.TRACK_ACCURACY, start.plusSeconds(30)),
                LocationFixture.createGpsHistory(wardKey, walked, LocationFixture.INACCURATE_ACCURACY, start.plusSeconds(60)),
                LocationFixture.createGpsHistory(wardKey, LocationFixture.LATITUDE + LocationFixture.JUMP_15KM_LATITUDE_DELTA,
                        LocationFixture.TRACK_ACCURACY, start.plusSeconds(90)),
                LocationFixture.createGpsHistory(wardKey, walkedTwice, LocationFixture.TRACK_ACCURACY, start.plusSeconds(120))));

        getHistoryAsToday(guardianJwtToken)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(3)
                .jsonPath("$.data[0].latitude").isEqualTo(LocationFixture.LATITUDE)
                .jsonPath("$.data[1].latitude").isEqualTo(walked)
                .jsonPath("$.data[2].latitude").isEqualTo(walkedTwice);
        assertThat(gpsHistoryRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /api/v1/location/history/{wardKey} - MANAGER 역할의 케어 관계여도 좌표가 반환된다")
    void getHistory_returns_recorded_coordinates_for_manager() {
        sendGps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE).expectStatus().isOk();

        getHistoryAsToday(managerJwtToken)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].latitude").isEqualTo(LocationFixture.LATITUDE)
                .jsonPath("$.data[0].longitude").isEqualTo(LocationFixture.LONGITUDE);
    }

    @Test
    @DisplayName("GET /api/v1/location/history/{wardKey} - WARD 본인은 케어 관계 없이도 자신의 경로를 조회한다")
    void getHistory_allows_ward_to_view_own_history() {
        sendGps(LocationFixture.LATITUDE, LocationFixture.LONGITUDE).expectStatus().isOk();

        getHistoryAsToday(wardJwtToken)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].latitude").isEqualTo(LocationFixture.LATITUDE);
    }

    @Test
    @DisplayName("GET /api/v1/location/history/{wardKey} - 인증 없이 요청하면 401이 반환된다")
    void getHistory_returns_401_without_auth() {
        client.get()
                .uri("/api/v1/location/history/{wardKey}?date={date}",
                        ward.getMemberKey(), LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/v1/location/history/{wardKey} - 케어 관계 없는 wardKey 조회 시 400 E5011이 반환된다")
    void getHistory_returns_400_for_unrelated_ward() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        client.get()
                .uri("/api/v1/location/history/{wardKey}?date={date}", "non-existent-ward-key", today)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianJwtToken)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");
    }

    // ── 이상탐지 사건 경로 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 종료된 사건은 시작~종료 구간 좌표를 시간순으로 돌려주고 다른 대상자 좌표는 섞지 않는다")
    void getAnomalyEventRoute_returns_points_within_ended_event_in_order() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        double first = LocationFixture.LATITUDE;
        double second = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA;
        double third = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA * 2;
        anomalyDetectionRepository.saveAll(List.of(
                LocationFixture.createEventStartDetection(wardKey, start),
                LocationFixture.createEventEndDetection(wardKey, start, LocationFixture.ROUTE_EVENT_ENDED_AT)));
        gpsHistoryRepository.saveAll(List.of(
                LocationFixture.createGpsHistoryRecordedAt(wardKey, third, LocationFixture.TRACK_ACCURACY, start.plusHours(2)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, first, LocationFixture.TRACK_ACCURACY, start.plusMinutes(10)),
                LocationFixture.createGpsHistoryRecordedAt(LocationFixture.OTHER_WARD_KEY, LocationFixture.LATITUDE,
                        LocationFixture.TRACK_ACCURACY, start.plusMinutes(30)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, second, LocationFixture.TRACK_ACCURACY, start.plusHours(1))));

        getAnomalyEventRoute(guardianJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.eventId").isEqualTo(LocationFixture.anomalyEventId(wardKey, start))
                .jsonPath("$.data.startedAt").isEqualTo(LocationFixture.ROUTE_EVENT_STARTED_AT_INSTANT_TEXT)
                .jsonPath("$.data.endedAt").isEqualTo(LocationFixture.ROUTE_EVENT_ENDED_AT_INSTANT_TEXT)
                .jsonPath("$.data.ongoing").isEqualTo(false)
                .jsonPath("$.data.route.length()").isEqualTo(3)
                .jsonPath("$.data.route[0].latitude").isEqualTo(first)
                .jsonPath("$.data.route[1].latitude").isEqualTo(second)
                .jsonPath("$.data.route[2].latitude").isEqualTo(third);
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 자정을 넘는 사건은 두 날짜의 좌표를 모두 돌려준다")
    void getAnomalyEventRoute_returns_points_across_midnight() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.OVERNIGHT_EVENT_STARTED_AT;
        double beforeMidnight = LocationFixture.LATITUDE;
        double afterMidnight = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA;
        anomalyDetectionRepository.saveAll(List.of(
                LocationFixture.createEventStartDetection(wardKey, start),
                LocationFixture.createEventEndDetection(wardKey, start, LocationFixture.OVERNIGHT_EVENT_ENDED_AT)));
        gpsHistoryRepository.saveAll(List.of(
                LocationFixture.createGpsHistoryRecordedAt(wardKey, beforeMidnight, LocationFixture.TRACK_ACCURACY, start.plusMinutes(15)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, afterMidnight, LocationFixture.TRACK_ACCURACY, start.plusMinutes(45))));

        getAnomalyEventRoute(guardianJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.route.length()").isEqualTo(2)
                .jsonPath("$.data.route[0].latitude").isEqualTo(beforeMidnight)
                .jsonPath("$.data.route[1].latitude").isEqualTo(afterMidnight);
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 정확도가 낮은 좌표는 경로에서 뺀다")
    void getAnomalyEventRoute_excludes_inaccurate_points() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        double accurate = LocationFixture.LATITUDE;
        double inaccurate = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA;
        double accurateLater = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA * 2;
        anomalyDetectionRepository.saveAll(List.of(
                LocationFixture.createEventStartDetection(wardKey, start),
                LocationFixture.createEventEndDetection(wardKey, start, LocationFixture.ROUTE_EVENT_ENDED_AT)));
        gpsHistoryRepository.saveAll(List.of(
                LocationFixture.createGpsHistoryRecordedAt(wardKey, accurate, LocationFixture.TRACK_ACCURACY, start.plusMinutes(10)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, inaccurate, LocationFixture.INACCURATE_ACCURACY, start.plusMinutes(20)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, accurateLater, LocationFixture.TRACK_ACCURACY, start.plusMinutes(30))));

        getAnomalyEventRoute(guardianJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.route.length()").isEqualTo(2)
                .jsonPath("$.data.route[0].latitude").isEqualTo(accurate)
                .jsonPath("$.data.route[1].latitude").isEqualTo(accurateLater);
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 시작·종료 시각과 같은 좌표는 포함하고 1초 밖의 좌표는 뺀다")
    void getAnomalyEventRoute_includes_points_exactly_at_bounds() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        LocalDateTime end = LocationFixture.ROUTE_EVENT_ENDED_AT;
        double beforeStart = LocationFixture.LATITUDE;
        double atStart = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA;
        double atEnd = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA * 2;
        double afterEnd = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA * 3;
        anomalyDetectionRepository.saveAll(List.of(
                LocationFixture.createEventStartDetection(wardKey, start),
                LocationFixture.createEventEndDetection(wardKey, start, end)));
        gpsHistoryRepository.saveAll(List.of(
                LocationFixture.createGpsHistoryRecordedAt(wardKey, beforeStart, LocationFixture.TRACK_ACCURACY, start.minusSeconds(1)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, atStart, LocationFixture.TRACK_ACCURACY, start),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, atEnd, LocationFixture.TRACK_ACCURACY, end),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, afterEnd, LocationFixture.TRACK_ACCURACY, end.plusSeconds(1))));

        getAnomalyEventRoute(guardianJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.route.length()").isEqualTo(2)
                .jsonPath("$.data.route[0].latitude").isEqualTo(atStart)
                .jsonPath("$.data.route[1].latitude").isEqualTo(atEnd);
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 종료 기록이 없는 사건은 마지막 알림 수신 시각까지만 포함하고 진행 중으로 응답한다")
    void getAnomalyEventRoute_ends_ongoing_event_at_last_alert() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        LocalDateTime lastAlertAt = start.plusHours(2);
        double early = LocationFixture.LATITUDE;
        double atLastAlert = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA;
        double afterLastAlert = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA * 2;
        insertSpeedDetection(wardKey, AnomalyAlertKind.EVENT_START, start, start, null);
        insertSpeedDetection(wardKey, AnomalyAlertKind.REMIND, start, lastAlertAt, null);
        gpsHistoryRepository.saveAll(List.of(
                LocationFixture.createGpsHistoryRecordedAt(wardKey, early, LocationFixture.TRACK_ACCURACY, start.plusHours(1)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, atLastAlert, LocationFixture.TRACK_ACCURACY, lastAlertAt),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, afterLastAlert, LocationFixture.TRACK_ACCURACY,
                        lastAlertAt.plusSeconds(1))));

        getAnomalyEventRoute(guardianJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.ongoing").isEqualTo(true)
                .jsonPath("$.data.endedAt").doesNotExist()
                .jsonPath("$.data.route.length()").isEqualTo(2)
                .jsonPath("$.data.route[0].latitude").isEqualTo(early)
                .jsonPath("$.data.route[1].latitude").isEqualTo(atLastAlert);
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 종료 알림은 엔진의 종료 시각이 아니라 수신 시각까지 경로를 돌려준다")
    void getAnomalyEventRoute_ends_at_event_end_received_time() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        LocalDateTime receivedAt = LocationFixture.ROUTE_EVENT_ENDED_AT;
        LocalDateTime engineEndedAt = receivedAt.minusMinutes(10);
        double beforeEngineEnd = LocationFixture.LATITUDE;
        double betweenEnds = LocationFixture.LATITUDE + LocationFixture.WALK_50M_LATITUDE_DELTA;
        insertSpeedDetection(wardKey, AnomalyAlertKind.EVENT_START, start, start, null);
        insertSpeedDetection(wardKey, AnomalyAlertKind.EVENT_END, start, receivedAt, engineEndedAt);
        gpsHistoryRepository.saveAll(List.of(
                LocationFixture.createGpsHistoryRecordedAt(wardKey, beforeEngineEnd, LocationFixture.TRACK_ACCURACY,
                        engineEndedAt.minusMinutes(1)),
                LocationFixture.createGpsHistoryRecordedAt(wardKey, betweenEnds, LocationFixture.TRACK_ACCURACY,
                        receivedAt.minusMinutes(1))));

        getAnomalyEventRoute(guardianJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.ongoing").isEqualTo(false)
                .jsonPath("$.data.endedAt").isEqualTo(LocationFixture.ROUTE_EVENT_ENDED_AT_INSTANT_TEXT)
                .jsonPath("$.data.route.length()").isEqualTo(2)
                .jsonPath("$.data.route[0].latitude").isEqualTo(beforeEngineEnd)
                .jsonPath("$.data.route[1].latitude").isEqualTo(betweenEnds);
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 같은 좌표에서 나간 알림과 사건 종료가 모두 저장돼 경로가 종료 시각에서 끝난다")
    void getAnomalyEventRoute_ends_at_event_end_received_on_same_point_as_alert() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        LocalDateTime end = LocationFixture.ROUTE_EVENT_ENDED_AT;
        insertSpeedDetection(wardKey, AnomalyAlertKind.EVENT_START, start, start, null);
        insertSpeedDetection(wardKey, AnomalyAlertKind.CATEGORY_ADDED, start, end, null);
        insertSpeedDetection(wardKey, AnomalyAlertKind.EVENT_END, start, end, end);

        getAnomalyEventRoute(guardianJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.ongoing").isEqualTo(false)
                .jsonPath("$.data.endedAt").isEqualTo(LocationFixture.ROUTE_EVENT_ENDED_AT_INSTANT_TEXT);
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 없는 사건을 조회하면 404 E6003이 반환된다")
    void getAnomalyEventRoute_returns_404_for_unknown_event() {
        String wardKey = ward.getMemberKey();

        getAnomalyEventRoute(guardianJwtToken, wardKey,
                LocationFixture.anomalyEventId(wardKey, LocationFixture.ROUTE_EVENT_STARTED_AT))
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E6003");
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 관계 있는 대상자 경로로 다른 대상자의 사건을 조회하면 404 E6003이 반환된다")
    void getAnomalyEventRoute_returns_404_for_event_of_other_ward() {
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        anomalyDetectionRepository.saveAll(List.of(
                LocationFixture.createEventStartDetection(LocationFixture.OTHER_WARD_KEY, start),
                LocationFixture.createEventEndDetection(LocationFixture.OTHER_WARD_KEY, start, LocationFixture.ROUTE_EVENT_ENDED_AT)));

        getAnomalyEventRoute(guardianJwtToken, ward.getMemberKey(),
                LocationFixture.anomalyEventId(LocationFixture.OTHER_WARD_KEY, start))
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E6003");
    }

    @Test
    @DisplayName("GET /api/v1/location/anomaly-events/{wardKey}/route - 케어 관계가 없는 보호자가 조회하면 400 E5011이 반환된다")
    void getAnomalyEventRoute_returns_400_for_unrelated_guardian() {
        String wardKey = ward.getMemberKey();
        LocalDateTime start = LocationFixture.ROUTE_EVENT_STARTED_AT;
        anomalyDetectionRepository.saveAll(List.of(
                LocationFixture.createEventStartDetection(wardKey, start),
                LocationFixture.createEventEndDetection(wardKey, start, LocationFixture.ROUTE_EVENT_ENDED_AT)));
        Member unrelatedGuardian = memberRepository.save(LocationFixture.createCoGuardian());
        localAuthRepository.save(AuthFixture.createLocalAuth(
                unrelatedGuardian.getMemberKey(), passwordEncoder.encode(AuthFixture.RAW_PASSWORD)));
        String unrelatedJwtToken = extractAccessToken(unrelatedGuardian.getPhone(), AuthFixture.RAW_PASSWORD);

        getAnomalyEventRoute(unrelatedJwtToken, wardKey, LocationFixture.anomalyEventId(wardKey, start))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");
    }

    // ── SSE 스트림 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/location/stream/{wardKey} - 인증 없이 요청하면 401이 반환된다")
    void streamLocation_returns_401_without_auth() {
        client.get()
                .uri("/api/v1/location/stream/{wardKey}", ward.getMemberKey())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private void insertSpeedDetection(String wardKey, AnomalyAlertKind alertKind, LocalDateTime startedAt,
                                      LocalDateTime recordedAt, LocalDateTime endedAt) {
        transactionTemplate.executeWithoutResult(status -> anomalyDetectionRepository.insertIfAbsent(
                wardKey, DetectionType.SPEED_ANOMALY.name(), LocationFixture.ANOMALY_SCORE, recordedAt,
                LocationFixture.LATITUDE, LocationFixture.LONGITUDE, null,
                alertKind.name(), LocationFixture.anomalyEventId(wardKey, startedAt), startedAt, endedAt,
                DetectionType.SPEED_ANOMALY.name(), AnomalyReasonCode.VEHICLE.name(),
                null, null, null, null, null));
    }

    private RestTestClient.ResponseSpec sendGps(double latitude, double longitude) {
        return client.post()
                .uri("/api/v1/location/gps")
                .header(HttpHeaders.AUTHORIZATION, "Device " + wardDeviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"latitude": %s, "longitude": %s}
                        """.formatted(latitude, longitude))
                .exchange();
    }

    private RestTestClient.ResponseSpec getAnomalyEventRoute(String jwtToken, String wardKey, String eventId) {
        return client.get()
                .uri("/api/v1/location/anomaly-events/{wardKey}/route?eventId={eventId}", wardKey, eventId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .exchange();
    }

    private RestTestClient.ResponseSpec getHistoryAsToday(String jwtToken) {
        return client.get()
                .uri("/api/v1/location/history/{wardKey}?date={date}",
                        ward.getMemberKey(), LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .exchange();
    }

    private String extractAccessToken(String phone, String password) {
        byte[] body = client.post()
                .uri("/api/v1/auth/sign-in/local")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phone": "%s", "password": "%s"}
                        """.formatted(phone, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();
        String json = new String(body);
        int start = json.indexOf("\"accessToken\":\"") + 15;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
