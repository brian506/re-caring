package com.recaring.device.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeviceTokenController HTTP 통합 테스트")
class DeviceTokenControllerTest extends AbstractIntegrationTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private LocalAuthRepository localAuthRepository;
    @Autowired private WardDeviceTokenRepository wardDeviceTokenRepository;
    @Autowired private GpsHistoryRepository gpsHistoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member ward;
    private Member guardian;
    private String wardToken;
    private String guardianToken;

    @BeforeEach
    void setUp() {
        ward = memberRepository.save(LocationFixture.createWard());
        guardian = memberRepository.save(LocationFixture.createGuardian());

        String encoded = passwordEncoder.encode("password1");
        localAuthRepository.save(LocalAuth.of(ward.getMemberKey(), "ward@test.com", encoded));
        localAuthRepository.save(LocalAuth.of(guardian.getMemberKey(), "guardian@test.com", encoded));

        wardToken = extractAccessToken("ward@test.com", "password1");
        guardianToken = extractAccessToken("guardian@test.com", "password1");
    }

    @AfterEach
    void tearDown() {
        gpsHistoryRepository.deleteAllInBatch();
        wardDeviceTokenRepository.deleteAllInBatch();
        localAuthRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("POST /api/v1/device/token - WARD가 요청하면 발급된 토큰이 그대로 저장된다")
    void issueToken_returns_the_persisted_token_for_ward() {
        byte[] body = client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.deviceToken").isNotEmpty()
                .returnResult()
                .getResponseBody();

        String persisted = currentDeviceToken();
        assertThat(new String(body)).contains(persisted);
    }

    @Test
    @DisplayName("POST /api/v1/device/token - 인증 없이 요청하면 401이 반환된다")
    void issueToken_returns_401_without_auth() {
        client.post()
                .uri("/api/v1/device/token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/device/token - GUARDIAN이 요청하면 403이 반환된다")
    void issueToken_returns_403_for_guardian() {
        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /api/v1/device/token - 재발급하면 기존 행의 토큰이 교체되고 새 행은 생기지 않는다")
    void issueToken_replaces_token_without_inserting_new_row() {
        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk();
        String firstToken = currentDeviceToken();

        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk();
        String reissuedToken = currentDeviceToken();

        assertThat(reissuedToken).isNotEqualTo(firstToken);
        assertThat(wardDeviceTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("재발급하면 캐시에 올라와 있던 직전 토큰도 즉시 거부된다")
    void reissue_invalidates_the_cached_previous_token() {
        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk();
        String previousToken = currentDeviceToken();

        sendGps(previousToken).expectStatus().isOk();

        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk();

        sendGps(previousToken).expectStatus().isUnauthorized();
        sendGps(currentDeviceToken()).expectStatus().isOk();
    }

    private RestTestClient.ResponseSpec sendGps(String deviceToken) {
        return client.post()
                .uri("/api/v1/location/gps")
                .header(HttpHeaders.AUTHORIZATION, "Device " + deviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"latitude": 37.5665, "longitude": 126.9780}
                        """)
                .exchange();
    }

    private String currentDeviceToken() {
        return wardDeviceTokenRepository.findByWardKey(ward.getMemberKey())
                .orElseThrow()
                .getToken();
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
