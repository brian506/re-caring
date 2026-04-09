package com.recaring.care.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
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

@DisplayName("CareController HTTP 통합 테스트")
class CareControllerTest extends AbstractIntegrationTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private LocalAuthRepository localAuthRepository;
    @Autowired private CareInvitationRepository careInvitationRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member guardian;
    private Member ward;
    private String guardianToken;
    private String wardToken;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(CareFixture.createGuardianMember());
        ward = memberRepository.save(CareFixture.createWardMember());

        String encoded = passwordEncoder.encode("password1!");
        localAuthRepository.save(LocalAuth.of(guardian.getMemberKey(), "guardian@test.com", encoded));
        localAuthRepository.save(LocalAuth.of(ward.getMemberKey(), "ward@test.com", encoded));

        guardianToken = extractAccessToken("guardian@test.com", "password1!");
        wardToken = extractAccessToken("ward@test.com", "password1!");
    }

    @AfterEach
    void tearDown() {
        careRelationshipRepository.deleteAll();
        careInvitationRepository.deleteAll();
        localAuthRepository.deleteAll();
        memberRepository.deleteAll();
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
                .jsonPath("$.data.accessToken").isNotEmpty()
                .returnResult();
        // Extract accessToken from response body
        String body = new String(result.getResponseBody());
        int start = body.indexOf("\"accessToken\":\"") + 15;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    // ── 보호 대상자 초대 ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 인증 없이 요청하면 401이 반환된다")
    void requestAddWard_without_auth_returns_401() {
        client.post()
                .uri("/api/v1/care/requests/ward")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 유효한 WARD 전화번호로 초대 요청이 성공한다")
    void requestAddWard_success() {
        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 존재하지 않는 전화번호면 4xx가 반환된다")
    void requestAddWard_fails_when_phone_not_found() {
        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "01099999999"}
                        """)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    // ── 받은 요청 목록 조회 ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/care/requests/received - 받은 PENDING 요청 목록을 반환한다")
    void getReceivedRequests_success() {
        CareInvitation invitation = CareFixture.createWardInvitation(
                guardian.getMemberKey(), ward.getMemberKey());
        careInvitationRepository.save(invitation);

        client.get()
                .uri("/api/v1/care/requests/received")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].requestKey").isNotEmpty()
                .jsonPath("$.data[0].requesterName").isEqualTo("보호자");
    }

    @Test
    @DisplayName("GET /api/v1/care/requests/received - 인증 없이 요청하면 401이 반환된다")
    void getReceivedRequests_without_auth_returns_401() {
        client.get()
                .uri("/api/v1/care/requests/received")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── 요청 수락/거절 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 수락 시 케어 관계가 생성된다")
    void acceptRequest_success() {
        CareInvitation invitation = CareFixture.createWardInvitation(
                guardian.getMemberKey(), ward.getMemberKey());
        CareInvitation saved = careInvitationRepository.save(invitation);

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/reject - 거절 시 상태가 변경된다")
    void rejectRequest_success() {
        CareInvitation invitation = CareFixture.createWardInvitation(
                guardian.getMemberKey(), ward.getMemberKey());
        CareInvitation saved = careInvitationRepository.save(invitation);

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/reject")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + wardToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");
    }

    // ── 보호 대상자 목록 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/care/wards - 내가 보호자인 보호 대상자 목록을 반환한다")
    void getMyWards_success() {
        CareRelationship relationship = CareFixture.createGuardianRelationship(
                ward.getMemberKey(), guardian.getMemberKey());
        careRelationshipRepository.save(relationship);

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + guardianToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].wardName").isEqualTo("보호대상자");
    }

    // ── 보호자/관리자 목록 조회 ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/care/wards/{wardKey}/caregivers - 보호자 본인이 조회하면 성공한다")
    void getCaregivers_success_as_ward_self() {
        CareRelationship relationship = CareFixture.createGuardianRelationship(
                ward.getMemberKey(), guardian.getMemberKey());
        careRelationshipRepository.save(relationship);

        String token = extractAccessToken("ward@test.com", "password1!");

        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data[0].name").isEqualTo("보호자");
    }

    @Test
    @DisplayName("GET /api/v1/care/wards/{wardKey}/caregivers - 관계없는 사람이 조회하면 403이 반환된다")
    void getCaregivers_fails_when_unauthorized() {
        Member stranger = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        localAuthRepository.save(LocalAuth.of(stranger.getMemberKey(), "stranger@test.com",
                passwordEncoder.encode("password1!")));
        String strangerToken = extractAccessToken("stranger@test.com", "password1!");

        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken)
                .exchange()
                .expectStatus().isForbidden();
    }
}
