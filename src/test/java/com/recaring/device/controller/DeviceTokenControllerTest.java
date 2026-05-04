package com.recaring.device.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
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

@DisplayName("DeviceTokenController HTTP 통합 테스트")
class DeviceTokenControllerTest extends AbstractIntegrationTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private LocalAuthRepository localAuthRepository;
    @Autowired private WardDeviceTokenRepository wardDeviceTokenRepository;
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
        wardDeviceTokenRepository.deleteAllInBatch();
        localAuthRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("POST /api/v1/device/token - WARD가 요청하면 device token이 발급된다")
    void issueToken_returns_200_with_token_for_ward() {
        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.deviceToken").isNotEmpty();
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
        System.out.println("GUARDIAN JWT: [" + guardianToken + "]");
        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /api/v1/device/token - 두 번 요청해도 200이 반환된다 (재발급)")
    void issueToken_returns_200_on_second_request() {
        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/api/v1/device/token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.deviceToken").isNotEmpty();
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
