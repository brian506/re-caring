package com.recaring.care.controller;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRelationship;
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
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 수락 시 주보호자 케어 관계가 생성되고 요청은 ACCEPTED가 된다")
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
                ward.getMemberKey(), guardian.getMemberKey(), CareRole.PRIMARY_GUARDIAN)).isTrue();
        assertThat(statusOf(saved)).isEqualTo(CareInvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 주보호자가 있어도 관계자 요청 수락은 막히지 않는다")
    void acceptRequest_allows_manager_when_ward_has_primary_guardian() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        CareInvitation saved = careInvitationRepository.save(CareFixture.createManagerInvitation(
                guardian.getMemberKey(), manager.getMemberKey(), ward.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRoleOf(manager)).isEqualTo(CareRole.MANAGER);
        assertThat(statusOf(saved)).isEqualTo(CareInvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 주보호자가 있어도 보호자 요청 수락은 막히지 않는다")
    void acceptRequest_allows_guardian_when_ward_has_primary_guardian() {
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        CareInvitation saved = careInvitationRepository.save(CareFixture.createGuardianInvitation(
                guardian.getMemberKey(), coGuardian.getMemberKey(), ward.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, authHeader(coGuardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRoleOf(coGuardian)).isEqualTo(CareRole.GUARDIAN);
        assertThat(statusOf(saved)).isEqualTo(CareInvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 주보호자가 없는 대상자에 관계자 요청을 수락하면 주보호자로 임명된다")
    void acceptRequest_promotes_to_primary_guardian_when_ward_has_none() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        CareInvitation saved = careInvitationRepository.save(CareFixture.createManagerInvitation(
                guardian.getMemberKey(), manager.getMemberKey(), ward.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRoleOf(manager)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 이미 주보호자가 있으면 먼저 받아둔 요청을 수락해도 두 번째 주보호자가 생기지 않는다")
    void acceptRequest_rejects_second_primary_guardian() {
        Member other = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        CareInvitation pending = careInvitationRepository.save(
                CareFixture.createWardInvitation(other.getMemberKey(), ward.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/requests/" + pending.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, authHeader(ward))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5018");

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), other.getMemberKey())).isEmpty();
        assertThat(statusOf(pending)).isEqualTo(CareInvitationStatus.PENDING);
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 이미 주보호자가 있는 대상자에게는 요청을 보낼 수 없다")
    void requestAddWard_fails_when_ward_already_has_primary_guardian() {
        Member other = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(other))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5018");

        assertThat(careInvitationRepository.findAll()).isEmpty();
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
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

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
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

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
    @DisplayName("GET /api/v1/care/wards/{wardKey}/caregivers - 주보호자가 아닌 보호자도 조회할 수 있다")
    void getCaregivers_success_as_co_guardian() {
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), coGuardian.getMemberKey()));

        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers")
                .header(HttpHeaders.AUTHORIZATION, authHeader(coGuardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.length()").isEqualTo(2);
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
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

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
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 주보호자가 삭제하면 남은 보호자가 주보호자로 승계되고 관계자의 관계는 유지된다")
    void removeWard_promotes_remaining_guardian_when_primary_guardian_leaves() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), coGuardian.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRelationshipRepository.findAllByWardMemberKey(ward.getMemberKey())).hasSize(2);
        assertThat(careRoleOf(coGuardian)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(careRoleOf(manager)).isEqualTo(CareRole.MANAGER);
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 주보호자가 삭제할 때 보호자가 없으면 관계자가 주보호자로 승계된다")
    void removeWard_promotes_remaining_manager_when_no_guardian_left() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRoleOf(manager)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 주보호자 승계는 다른 보호 대상자의 케어 관계를 건드리지 않는다")
    void removeWard_succession_is_scoped_to_the_target_ward() {
        Member otherWard = memberRepository.save(CareFixture.createWardMember("01088889999"));
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(otherWard.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(otherWard.getMemberKey(), manager.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRoleOf(ward, manager)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(careRoleOf(otherWard, manager)).isEqualTo(CareRole.MANAGER);
        assertThat(careRoleOf(otherWard, guardian)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 마지막 케어 관계가 끊기면 그 대상자의 초대만 사라지고 다른 대상자 초대는 남는다")
    void removeWard_clears_pending_invitations_only_for_the_emptied_ward() {
        Member otherWard = memberRepository.save(CareFixture.createWardMember("01088889999"));
        Member invitee = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(otherWard.getMemberKey(), guardian.getMemberKey()));
        CareInvitation targetWardInvitation = careInvitationRepository.save(CareFixture.createManagerInvitation(
                guardian.getMemberKey(), invitee.getMemberKey(), ward.getMemberKey()));
        CareInvitation otherWardInvitation = careInvitationRepository.save(CareFixture.createManagerInvitation(
                guardian.getMemberKey(), invitee.getMemberKey(), otherWard.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRelationshipRepository.findAllByWardMemberKey(ward.getMemberKey())).isEmpty();
        assertThat(careInvitationRepository.findByRequestKey(targetWardInvitation.getRequestKey())).isEmpty();
        assertThat(careInvitationRepository.findByRequestKey(otherWardInvitation.getRequestKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 떠나는 사람이 보낸 대기 중 초대는 케어 관계가 남아 있어도 사라진다")
    void removeWard_clears_pending_invitations_sent_by_the_leaver() {
        Member coPrimary = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member invitee = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        Member otherInvitee = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), coPrimary.getMemberKey()));
        CareInvitation sentByLeaver = careInvitationRepository.save(CareFixture.createManagerInvitation(
                guardian.getMemberKey(), invitee.getMemberKey(), ward.getMemberKey()));
        CareInvitation sentByRemaining = careInvitationRepository.save(CareFixture.createManagerInvitation(
                coPrimary.getMemberKey(), otherInvitee.getMemberKey(), ward.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findByRequestKey(sentByLeaver.getRequestKey())).isEmpty();
        assertThat(careInvitationRepository.findByRequestKey(sentByRemaining.getRequestKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 주보호자가 아닌 보호자가 삭제하면 자기 관계만 빠진다")
    void removeWard_deletes_only_own_relationship_when_co_guardian() {
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), coGuardian.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(coGuardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), coGuardian.getMemberKey())).isEmpty();
        assertThat(careRoleOf(guardian)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 관계자 본인이 삭제하면 자기 관계만 빠지고 주보호자는 남는다")
    void removeWard_deletes_only_own_relationship_when_manager() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), manager.getMemberKey())).isEmpty();
        assertThat(careRoleOf(guardian)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
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
    @DisplayName("DELETE /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey} - 주보호자가 관리자를 삭제하면 관리자 관계만 제거된다")
    void removeCaregiver_deletes_only_target_relationship() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
    @DisplayName("DELETE /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey} - 주보호자가 아니면 403이 반환되고 관계가 남는다")
    void removeCaregiver_fails_when_not_primary_guardian_role() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + guardian.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5014");

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), guardian.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey} - 주보호자는 삭제할 수 없고 400 E5017이 반환된다")
    void removeCaregiver_fails_when_target_is_primary_guardian() {
        Member otherPrimary = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), otherPrimary.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + otherPrimary.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5017");

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), otherPrimary.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey} - 인증 없이 요청하면 401이 반환된다")
    void removeCaregiver_without_auth_returns_401() {
        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + guardian.getMemberKey())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── 보호 대상자 별명 수정 ──────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/nickname - 별명을 설정하면 내 관계 행에만 저장되고 다른 보호자 행은 그대로다")
    void updateWardNickname_saves_only_on_requester_relationship() {
        Member otherGuardian = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), otherGuardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/nickname")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nickname": "할머니"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(nicknameOf(guardian)).isEqualTo("할머니");
        assertThat(nicknameOf(otherGuardian)).isNull();
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/nickname - 빈 문자열을 보내면 별명이 해제된다")
    void updateWardNickname_clears_nickname_when_blank() {
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        patchNickname(guardian, "할머니");

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/nickname")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nickname": ""}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(nicknameOf(guardian)).isNull();
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/nickname - 별명이 20자면 저장된다")
    void updateWardNickname_accepts_nickname_at_max_length() {
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        String maxLengthNickname = "가".repeat(20);

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/nickname")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nickname": "%s"}
                        """.formatted(maxLengthNickname))
                .exchange()
                .expectStatus().isOk();

        assertThat(nicknameOf(guardian)).isEqualTo(maxLengthNickname);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/nickname - 별명이 21자면 400이고 저장되지 않는다")
    void updateWardNickname_fails_when_nickname_too_long() {
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/nickname")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nickname": "%s"}
                        """.formatted("가".repeat(21)))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(nicknameOf(guardian)).isNull();
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/nickname - 관계자도 별명을 설정할 수 있다")
    void updateWardNickname_allowed_for_manager() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/nickname")
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nickname": "이모님"}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(nicknameOf(manager)).isEqualTo("이모님");
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/nickname - 케어 관계가 없으면 400 E5011이 반환된다")
    void updateWardNickname_fails_when_relationship_not_found() {
        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/nickname")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nickname": "할머니"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");

        assertThat(careRelationshipRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/care/wards - 별명을 설정하면 실명과 별명이 함께 내려간다")
    void getMyWards_returns_real_name_and_nickname() {
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        patchNickname(guardian, "할머니");

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].wardName").isEqualTo("보호대상자")
                .jsonPath("$.data[0].wardNickname").isEqualTo("할머니")
                .jsonPath("$.data[0].myRole").isEqualTo("PRIMARY_GUARDIAN");
    }

    // ── 보호자/관계자 관계 수정 ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/care/requests/manager - 대상자에 연결된 사람이 이미 5명이면 400 E5000이고 초대가 생기지 않는다")
    void sendManagerInvitation_fails_when_ward_is_full() {
        memberRepository.save(CareFixture.createGuardianMember("01099990000"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(CareFixture.createManagerRelationship(
                ward.getMemberKey(), memberRepository.save(CareFixture.createGuardianMember("01011110001")).getMemberKey()));
        careRelationshipRepository.save(CareFixture.createManagerRelationship(
                ward.getMemberKey(), memberRepository.save(CareFixture.createGuardianMember("01011110002")).getMemberKey()));
        careRelationshipRepository.save(CareFixture.createManagerRelationship(
                ward.getMemberKey(), memberRepository.save(CareFixture.createGuardianMember("01011110003")).getMemberKey()));
        careRelationshipRepository.save(CareFixture.createGuardianRelationship(
                ward.getMemberKey(), memberRepository.save(CareFixture.createGuardianMember("01011110004")).getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/manager")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "01099990000", "wardMemberKey": "%s"}
                        """.formatted(ward.getMemberKey()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5000");

        assertThat(careInvitationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/manager - 대상자에 연결된 사람이 4명이면 초대가 생성된다")
    void sendManagerInvitation_succeeds_at_one_below_the_ward_limit() {
        Member invitee = memberRepository.save(CareFixture.createGuardianMember("01099990000"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(CareFixture.createManagerRelationship(
                ward.getMemberKey(), memberRepository.save(CareFixture.createGuardianMember("01011110001")).getMemberKey()));
        careRelationshipRepository.save(CareFixture.createManagerRelationship(
                ward.getMemberKey(), memberRepository.save(CareFixture.createGuardianMember("01011110002")).getMemberKey()));
        careRelationshipRepository.save(CareFixture.createGuardianRelationship(
                ward.getMemberKey(), memberRepository.save(CareFixture.createGuardianMember("01011110004")).getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/manager")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "01099990000", "wardMemberKey": "%s"}
                        """.formatted(ward.getMemberKey()))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findAll())
                .singleElement()
                .satisfies(invitation -> {
                    assertThat(invitation.getTargetMemberKey()).isEqualTo(invitee.getMemberKey());
                    assertThat(invitation.getWardMemberKey()).isEqualTo(ward.getMemberKey());
                    assertThat(invitation.getCareRole()).isEqualTo(CareRole.MANAGER);
                });
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/role - 주보호자가 보호자를 관계자로 바꾸면 역할이 MANAGER가 된다")
    void updateCaregiverRole_changes_guardian_to_manager() {
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), coGuardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + coGuardian.getMemberKey() + "/role")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"careRole": "MANAGER"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(careRoleOf(coGuardian)).isEqualTo(CareRole.MANAGER);
        assertThat(careRoleOf(guardian)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/role - 요청자가 주보호자가 아니면 403 E5014이고 역할이 그대로다")
    void updateCaregiverRole_fails_when_requester_not_primary_guardian() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + manager.getMemberKey() + "/role")
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"careRole": "GUARDIAN"}
                        """)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5014");

        assertThat(careRoleOf(manager)).isEqualTo(CareRole.MANAGER);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/role - 대상이 주보호자면 400 E5015이고 역할이 그대로다")
    void updateCaregiverRole_fails_when_target_is_primary_guardian() {
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + guardian.getMemberKey() + "/role")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"careRole": "MANAGER"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5015");

        assertThat(careRoleOf(guardian)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/role - 주보호자가 관계자를 주보호자로 올리면 주보호자가 두 명이 된다")
    void updateCaregiverRole_promotes_manager_to_primary_guardian() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + manager.getMemberKey() + "/role")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"careRole": "PRIMARY_GUARDIAN"}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(careRoleOf(manager)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(careRoleOf(guardian)).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/role - 승격된 주보호자도 다른 사람의 관계를 바꿀 수 있다")
    void updateCaregiverRole_allows_a_promoted_primary_guardian_to_manage_others() {
        Member promoted = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createPrimaryGuardianRelationship(ward.getMemberKey(), promoted.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), coGuardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + coGuardian.getMemberKey() + "/role")
                .header(HttpHeaders.AUTHORIZATION, authHeader(promoted))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"careRole": "MANAGER"}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(careRoleOf(coGuardian)).isEqualTo(CareRole.MANAGER);
    }

    private void patchNickname(Member caregiver, String nickname) {
        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/nickname")
                .header(HttpHeaders.AUTHORIZATION, authHeader(caregiver))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"nickname": "%s"}
                        """.formatted(nickname))
                .exchange()
                .expectStatus().isOk();
    }

    private String nicknameOf(Member caregiver) {
        return relationshipOf(caregiver).getWardNickname();
    }

    private CareRole careRoleOf(Member caregiver) {
        return relationshipOf(caregiver).getCareRole();
    }

    private CareRole careRoleOf(Member targetWard, Member caregiver) {
        return careRelationshipRepository
                .findCareRelationship(targetWard.getMemberKey(), caregiver.getMemberKey())
                .orElseThrow(() -> new AssertionError("케어 관계가 존재하지 않는다"))
                .getCareRole();
    }

    private CareRelationship relationshipOf(Member caregiver) {
        return careRelationshipRepository
                .findCareRelationship(ward.getMemberKey(), caregiver.getMemberKey())
                .orElseThrow(() -> new AssertionError("케어 관계가 존재하지 않는다"));
    }

    private CareInvitationStatus statusOf(CareInvitation invitation) {
        return careInvitationRepository.findByRequestKey(invitation.getRequestKey())
                .orElseThrow(() -> new AssertionError("케어 요청이 존재하지 않는다"))
                .getStatus();
    }
}
