package com.recaring.safezone.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("SafeZoneController HTTP 통합 테스트")
class SafeZoneControllerTest extends AbstractIntegrationTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private LocalAuthRepository localAuthRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;
    @Autowired private SafeZoneRepository safeZoneRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member guardian;
    private Member ward;
    private String guardianToken;
    private CareRelationship relationship;
    private SafeZone savedZone;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(CareFixture.createGuardianMember());
        ward = memberRepository.save(CareFixture.createWardMember());

        String encoded = passwordEncoder.encode("Password1");
        localAuthRepository.save(LocalAuth.of(guardian.getMemberKey(), "guardian@test.com", encoded));

        guardianToken = extractAccessToken("guardian@test.com", "Password1");
        relationship = careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        savedZone = safeZoneRepository.save(SafeZoneFixture.createSafeZone(ward.getMemberKey()));
    }

    @AfterEach
    void tearDown() {
        safeZoneRepository.deleteAllInBatch();
        careRelationshipRepository.deleteAllInBatch();
        localAuthRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    private String extractAccessToken(String email, String password) {
        var result = client.post()
                .uri("/api/v1/auth/sign-in/local")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();
        String body = new String(result.getResponseBody());
        int start = body.indexOf("\"accessToken\":\"") + 15;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    // ── POST /api/v1/care/wards/{wardKey}/safe-zones ─────────────────────────

    @Test
    @DisplayName("POST - 보호자가 안심존을 추가하면 201이 반환된다")
    void addSafeZone_success() {
        client.post()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "name": "학교",
                          "address": "서울시 마포구 1",
                          "latitude": 37.55,
                          "longitude": 126.92,
                          "radius": "SMALL"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST - 인증 없이 요청하면 401이 반환된다")
    void addSafeZone_without_auth_returns_401() {
        client.post()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "학교", "address": "서울시", "latitude": 37.5, "longitude": 127.0, "radius": "SMALL"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST - 케어 관계가 없는 보호자가 요청하면 403이 반환된다")
    void addSafeZone_returns_403_when_not_caregiver() {
        Member stranger = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        localAuthRepository.save(LocalAuth.of(stranger.getMemberKey(), "stranger@test.com",
                passwordEncoder.encode("Password1")));
        String strangerToken = extractAccessToken("stranger@test.com", "Password1");

        client.post()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "학교", "address": "서울시", "latitude": 37.5, "longitude": 127.0, "radius": "SMALL"}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── GET /api/v1/care/wards/{wardKey}/safe-zones ──────────────────────────

    @Test
    @DisplayName("GET 목록 - 보호자가 조회하면 안심존 목록이 반환된다")
    void getSafeZones_returns_list() {
        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].name").isEqualTo(SafeZoneFixture.NAME)
                .jsonPath("$.data[0].safeZoneKey").isNotEmpty();
    }

    @Test
    @DisplayName("GET 목록 - 인증 없이 요청하면 401이 반환된다")
    void getSafeZones_without_auth_returns_401() {
        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── GET /api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey} ────────────

    @Test
    @DisplayName("GET 상세 - 보호자가 조회하면 안심존 상세가 반환된다")
    void getSafeZone_returns_detail() {
        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + savedZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.name").isEqualTo(SafeZoneFixture.NAME)
                .jsonPath("$.data.address").isEqualTo(SafeZoneFixture.ADDRESS)
                .jsonPath("$.data.radiusMeters").isEqualTo(SafeZoneFixture.RADIUS.getMeters());
    }

    @Test
    @DisplayName("GET 상세 - 존재하지 않는 key를 요청하면 4xx가 반환된다")
    void getSafeZone_returns_4xx_when_not_found() {
        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/non-existent-key")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    // ── PATCH /api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey} ──────────

    @Test
    @DisplayName("PATCH - 보호자가 안심존을 수정하면 200이 반환된다")
    void updateSafeZone_success() {
        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + savedZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "name": "직장",
                          "address": "서울시 서초구 반포대로 2",
                          "latitude": 37.49,
                          "longitude": 127.01,
                          "radius": "LARGE"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("PATCH - 케어 관계가 없는 보호자가 수정하면 403이 반환된다")
    void updateSafeZone_returns_403_when_not_caregiver() {
        Member stranger = memberRepository.save(CareFixture.createGuardianMember("01088889999"));
        localAuthRepository.save(LocalAuth.of(stranger.getMemberKey(), "stranger2@test.com",
                passwordEncoder.encode("Password1")));
        String strangerToken = extractAccessToken("stranger2@test.com", "Password1");

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + savedZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "직장", "address": "서울시", "latitude": 37.5, "longitude": 127.0, "radius": "LARGE"}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── DELETE /api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey} ─────────

    @Test
    @DisplayName("DELETE - 보호자가 안심존을 삭제하면 200이 반환된다")
    void deleteSafeZone_success() {
        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + savedZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("DELETE - 케어 관계가 없는 보호자가 삭제하면 403이 반환된다")
    void deleteSafeZone_returns_403_when_not_caregiver() {
        Member stranger = memberRepository.save(CareFixture.createGuardianMember("01011110000"));
        localAuthRepository.save(LocalAuth.of(stranger.getMemberKey(), "stranger3@test.com",
                passwordEncoder.encode("Password1")));
        String strangerToken = extractAccessToken("stranger3@test.com", "Password1");

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/safe-zones/" + savedZone.getSafeZoneKey())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isForbidden();
    }
}
