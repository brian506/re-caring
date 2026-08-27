package com.recaring.location.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.location.dataaccess.repository.GpsHistoryRepository;
import com.recaring.location.fixture.LocationFixture;
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

@DisplayName("LocationController HTTP 통합 테스트")
class LocationControllerTest extends AbstractIntegrationTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private LocalAuthRepository localAuthRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;
    @Autowired private WardDeviceTokenRepository wardDeviceTokenRepository;
    @Autowired private GpsHistoryRepository gpsHistoryRepository;
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

        String encoded = passwordEncoder.encode("password1");
        localAuthRepository.save(LocalAuth.of(ward.getMemberKey(), "ward@location-test.com", encoded));
        localAuthRepository.save(LocalAuth.of(guardian.getMemberKey(), "guardian@location-test.com", encoded));
        localAuthRepository.save(LocalAuth.of(manager.getMemberKey(), "manager@location-test.com", encoded));

        guardianJwtToken = extractAccessToken("guardian@location-test.com", "password1");
        managerJwtToken = extractAccessToken("manager@location-test.com", "password1");
        wardJwtToken = extractAccessToken("ward@location-test.com", "password1");

        WardDeviceToken deviceToken = WardDeviceToken.builder().wardKey(ward.getMemberKey()).build();
        wardDeviceTokenRepository.save(deviceToken);
        wardDeviceToken = deviceToken.getToken();

        careRelationshipRepository.save(CareRelationship.of(
                ward.getMemberKey(), guardian.getMemberKey(), CareRole.GUARDIAN));
        careRelationshipRepository.save(CareRelationship.of(
                ward.getMemberKey(), manager.getMemberKey(), CareRole.MANAGER));
    }

    @AfterEach
    void tearDown() {
        careRelationshipRepository.deleteAllInBatch();
        gpsHistoryRepository.deleteAllInBatch();
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
                .body("""
                        {"latitude": 37.5665, "longitude": 126.9780}
                        """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /api/v1/location/gps - Device Token 없이 요청하면 401이 반환된다")
    void receiveGps_returns_401_without_device_token() {
        client.post()
                .uri("/api/v1/location/gps")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"latitude": 37.5665, "longitude": 126.9780}
                        """)
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
                .body("""
                        {"latitude": 37.5665, "longitude": 126.9780}
                        """)
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
                .body("""
                        {"latitude": 200.0, "longitude": 126.9780}
                        """)
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
    @DisplayName("GET /api/v1/location/history/{wardKey} - 케어 관계 없는 wardKey 조회 시 403이 반환된다")
    void getHistory_returns_403_for_unrelated_ward() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        client.get()
                .uri("/api/v1/location/history/{wardKey}?date={date}", "non-existent-ward-key", today)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianJwtToken)
                .exchange()
                .expectStatus().isForbidden();
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

    private RestTestClient.ResponseSpec getHistoryAsToday(String jwtToken) {
        return client.get()
                .uri("/api/v1/location/history/{wardKey}?date={date}",
                        ward.getMemberKey(), LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .exchange();
    }

    private String extractAccessToken(String email, String password) {
        byte[] body = client.post()
                .uri("/api/v1/auth/sign-in/local")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
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
