package com.recaring.care.controller;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRole;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CareController HTTP 통합 테스트")
class CareControllerTest extends AbstractIntegrationTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private CareInvitationRepository careInvitationRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;

    private Member guardian;
    private Member ward;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(CareFixture.createGuardianMember());
        ward = memberRepository.save(CareFixture.createWardMember());
    }

    @AfterEach
    void tearDown() {
        careRelationshipRepository.deleteAllInBatch();
        careInvitationRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    private String authHeader(Member member) {
        return bearerToken(member.getMemberKey(), member.getRole());
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
    @DisplayName("POST /api/v1/care/requests/ward - 초대 요청 시 대상자를 수신자로 하는 PENDING 요청이 생성된다")
    void requestAddWard_creates_pending_invitation() {
        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        List<CareInvitation> invitations = careInvitationRepository.findAll();
        assertThat(invitations).hasSize(1);
        CareInvitation created = invitations.get(0);
        assertThat(created.getRequesterMemberKey()).isEqualTo(guardian.getMemberKey());
        assertThat(created.getTargetMemberKey()).isEqualTo(ward.getMemberKey());
        assertThat(created.getWardMemberKey()).isEqualTo(ward.getMemberKey());
        assertThat(created.getStatus()).isEqualTo(CareInvitationStatus.PENDING);
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 존재하지 않는 전화번호면 4xx가 반환되고 요청이 생성되지 않는다")
    void requestAddWard_fails_when_phone_not_found() {
        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "01099999999"}
                        """)
                .exchange()
                .expectStatus().is4xxClientError();

        assertThat(careInvitationRepository.findAll()).isEmpty();
    }

    // ── 받은 요청 목록 조회 ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/care/requests/received - 받은 PENDING 요청 목록을 반환한다")
    void getReceivedRequests_success() {
        careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.get()
                .uri("/api/v1/care/requests/received")
                .header(HttpHeaders.AUTHORIZATION, authHeader(ward))
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
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 수락 시 GUARDIAN 케어 관계가 생성되고 요청은 ACCEPTED가 된다")
    void acceptRequest_creates_relationship_and_marks_accepted() {
        CareInvitation saved = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, authHeader(ward))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(careRelationshipRepository.existsCareRelationship(
                ward.getMemberKey(), guardian.getMemberKey(), CareRole.GUARDIAN)).isTrue();
        assertThat(statusOf(saved)).isEqualTo(CareInvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/reject - 거절 시 요청은 REJECTED가 되고 케어 관계는 생기지 않는다")
    void rejectRequest_marks_rejected_without_relationship() {
        CareInvitation saved = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/reject")
                .header(HttpHeaders.AUTHORIZATION, authHeader(ward))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(statusOf(saved)).isEqualTo(CareInvitationStatus.REJECTED);
        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), guardian.getMemberKey())).isEmpty();
    }

    // ── 보호 대상자 목록 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/care/wards - 내가 보호자인 보호 대상자 목록을 반환한다")
    void getMyWards_success() {
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].wardName").isEqualTo("보호대상자")
                .jsonPath("$.data[0].wardGender").isEqualTo("FEMALE");
    }

    // ── 보호자/관리자 목록 조회 ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/care/wards/{wardKey}/caregivers - 보호자 본인이 조회하면 성공한다")
    void getCaregivers_success_as_ward_self() {
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers")
                .header(HttpHeaders.AUTHORIZATION, authHeader(ward))
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

        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers")
                .header(HttpHeaders.AUTHORIZATION, authHeader(stranger))
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── 보호 대상자 케어 관계 삭제 ─────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 보호자가 삭제하면 케어 관계 행이 제거된다")
    void removeWard_deletes_relationship_row() {
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), guardian.getMemberKey())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 케어 관계가 없으면 NOT_FOUND_CARE_RELATIONSHIP이 반환된다")
    void removeWard_fails_when_relationship_not_found() {
        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 인증 없이 요청하면 401이 반환된다")
    void removeWard_without_auth_returns_401() {
        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── 보호자/관계자 케어 관계 삭제 ───────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey} - GUARDIAN이 관리자를 삭제하면 관리자 관계만 제거된다")
    void removeCaregiver_deletes_only_target_relationship() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + manager.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), manager.getMemberKey())).isEmpty();
        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), guardian.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey} - GUARDIAN 역할이 아니면 403이 반환되고 관계가 남는다")
    void removeCaregiver_fails_when_not_guardian_role() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + guardian.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5012");

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), guardian.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey} - 인증 없이 요청하면 401이 반환된다")
    void removeCaregiver_without_auth_returns_401() {
        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + guardian.getMemberKey())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private CareInvitationStatus statusOf(CareInvitation invitation) {
        return careInvitationRepository.findByRequestKey(invitation.getRequestKey())
                .orElseThrow(() -> new AssertionError("케어 요청이 존재하지 않는다"))
                .getStatus();
    }
}
