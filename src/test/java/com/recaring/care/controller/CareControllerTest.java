package com.recaring.care.controller;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.vo.CareRole;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.dataaccess.repository.DesignatedAvatarRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("CareController HTTP 통합 테스트")
class CareControllerTest extends AbstractIntegrationTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private CareInvitationRepository careInvitationRepository;
    @Autowired private CareRelationshipRepository careRelationshipRepository;
    @Autowired private DesignatedAvatarRepository designatedAvatarRepository;
    @Autowired private NotificationSettingRepository notificationSettingRepository;
    @Autowired private LocationSettingRepository locationSettingRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member guardian;
    private Member ward;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(CareFixture.createGuardianMember());
        ward = memberRepository.save(CareFixture.createWardMember());
    }

    @AfterEach
    void tearDown() {
        designatedAvatarRepository.deleteAllInBatch();
        notificationSettingRepository.deleteAllInBatch();
        locationSettingRepository.deleteAllInBatch();
        careRelationshipRepository.deleteAllInBatch();
        careInvitationRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        redisTemplate.delete(redisTemplate.keys("care:invitation:resend:*"));
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

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 이미 보호자가 있는 대상자에게도 PENDING 요청이 생성된다")
    void requestAddWard_creates_request_even_when_ward_already_has_guardian() {
        Member newGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(newGuardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findAll())
                .singleElement()
                .satisfies(invitation -> {
                    assertThat(invitation.getRequesterMemberKey()).isEqualTo(newGuardian.getMemberKey());
                    assertThat(invitation.getTargetMemberKey()).isEqualTo(ward.getMemberKey());
                    assertThat(invitation.getCareRole()).isEqualTo(CareRole.GUARDIAN);
                    assertThat(invitation.getStatus()).isEqualTo(CareInvitationStatus.PENDING);
                });
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 다른 보호자가 같은 대상자에게 대기 요청을 보내 둔 상태여도 요청이 생성된다")
    void requestAddWard_allows_request_while_another_guardian_request_is_pending() {
        Member otherRequester = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        careInvitationRepository.save(
                CareFixture.createWardInvitation(otherRequester.getMemberKey(), ward.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findAll())
                .extracting(CareInvitation::getRequesterMemberKey, CareInvitation::getStatus)
                .containsExactlyInAnyOrder(
                        tuple(otherRequester.getMemberKey(), CareInvitationStatus.PENDING),
                        tuple(guardian.getMemberKey(), CareInvitationStatus.PENDING));
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 대상자에 연결된 사람이 4명이면 요청이 생성된다")
    void requestAddWard_succeeds_at_one_below_the_ward_limit() {
        connectGuardians(ward, "01011110001", "01011110002", "01011110003", "01011110004");

        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findAll())
                .singleElement()
                .extracting(CareInvitation::getRequesterMemberKey)
                .isEqualTo(guardian.getMemberKey());
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 대상자에 연결된 사람이 5명이면 400 E5000이고 요청이 생기지 않는다")
    void requestAddWard_fails_when_ward_is_full() {
        connectGuardians(ward, "01011110001", "01011110002", "01011110003", "01011110004", "01011110005");

        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5000");

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
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 보호자가 있는 대상자에 관계자 요청을 수락하면 MANAGER로 연결된다")
    void acceptRequest_connects_manager_to_ward_with_guardian() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 신규 보호자가 보호자 추가 요청을 수락하면 GUARDIAN으로 연결된다")
    void acceptRequest_connects_new_guardian_by_guardian_invitation() {
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 보호자가 있는 대상자가 수락하면 GUARDIAN 관계가 추가되고 기존 보호자 관계는 그대로다")
    void acceptRequest_adds_second_guardian_and_keeps_existing_one() {
        Member newGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        CareInvitation saved = careInvitationRepository.save(
                CareFixture.createWardInvitation(newGuardian.getMemberKey(), ward.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/requests/" + saved.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, authHeader(ward))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRelationshipRepository.findAllByWardMemberKey(ward.getMemberKey()))
                .extracting(CareRelationship::getCaregiverMemberKey, CareRelationship::getCareRole)
                .containsExactlyInAnyOrder(
                        tuple(guardian.getMemberKey(), CareRole.GUARDIAN),
                        tuple(newGuardian.getMemberKey(), CareRole.GUARDIAN));
        assertThat(statusOf(saved)).isEqualTo(CareInvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/requests/{requestKey}/accept - 요청 후 대상자 인원이 5명이 되면 수락해도 400 E5000이고 관계 없이 요청은 PENDING이다")
    void acceptRequest_fails_when_ward_became_full_after_request() {
        Member newGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        CareInvitation pending = careInvitationRepository.save(
                CareFixture.createWardInvitation(newGuardian.getMemberKey(), ward.getMemberKey()));
        connectGuardians(ward, "01011110001", "01011110002", "01011110003", "01011110004", "01011110005");

        client.patch()
                .uri("/api/v1/care/requests/" + pending.getRequestKey() + "/accept")
                .header(HttpHeaders.AUTHORIZATION, authHeader(ward))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5000");

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), newGuardian.getMemberKey())).isEmpty();
        assertThat(statusOf(pending)).isEqualTo(CareInvitationStatus.PENDING);
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

    // ── 보낸 케어 요청 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/care/requests/sent - 내가 보낸 대기 요청만 최신순으로 받는 사람 번호·역할과 함께 반환한다")
    void getSentRequests_returns_my_pending_requests_newest_first() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member other = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        Member otherWard = memberRepository.save(CareFixture.createWardMember(CareFixture.OTHER_WARD_PHONE));
        CareInvitation olderWardRequest = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));
        CareInvitation newerManagerRequest = careInvitationRepository.save(
                CareFixture.createManagerInvitation(guardian.getMemberKey(), manager.getMemberKey(), ward.getMemberKey()));
        careInvitationRepository.save(
                CareFixture.createAcceptedWardInvitation(guardian.getMemberKey(), otherWard.getMemberKey()));
        careInvitationRepository.save(
                CareFixture.createWardInvitation(other.getMemberKey(), otherWard.getMemberKey()));
        overwriteCreatedAt(olderWardRequest, LocalDateTime.of(2026, 10, 5, 9, 0));
        overwriteCreatedAt(newerManagerRequest, LocalDateTime.of(2026, 10, 5, 10, 0));

        client.get()
                .uri("/api/v1/care/requests/sent")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].requestKey").isEqualTo(newerManagerRequest.getRequestKey())
                .jsonPath("$.data[0].phoneNumber").isEqualTo(CareFixture.MANAGER_PHONE)
                .jsonPath("$.data[0].targetRole").isEqualTo("MANAGER")
                .jsonPath("$.data[0].status").isEqualTo("PENDING")
                .jsonPath("$.data[1].requestKey").isEqualTo(olderWardRequest.getRequestKey())
                .jsonPath("$.data[1].phoneNumber").isEqualTo(CareFixture.WARD_PHONE)
                .jsonPath("$.data[1].targetRole").isEqualTo("WARD")
                .jsonPath("$.data[1].status").isEqualTo("PENDING");
    }

    @Test
    @DisplayName("GET /api/v1/care/requests/sent?status=REJECTED - 거절된 요청만 반환한다")
    void getSentRequests_filters_by_rejected_status() {
        Member otherWard = memberRepository.save(CareFixture.createWardMember(CareFixture.OTHER_WARD_PHONE));
        careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));
        CareInvitation rejected = careInvitationRepository.save(
                CareFixture.createRejectedWardInvitation(guardian.getMemberKey(), otherWard.getMemberKey()));

        client.get()
                .uri("/api/v1/care/requests/sent?status=REJECTED")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].requestKey").isEqualTo(rejected.getRequestKey())
                .jsonPath("$.data[0].status").isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("GET /api/v1/care/requests/sent - 대기 중인 대상자 추가 요청은 대상자 키·이름을 내려주지 않는다")
    void getSentRequests_hides_ward_of_pending_ward_request() {
        careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.get()
                .uri("/api/v1/care/requests/sent")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].wardKey").isEmpty()
                .jsonPath("$.data[0].wardName").isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/care/requests/sent?status=ACCEPTED - 수락된 대상자 추가 요청은 대상자 키·이름을 내려준다")
    void getSentRequests_reveals_ward_of_accepted_ward_request() {
        careInvitationRepository.save(
                CareFixture.createAcceptedWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.get()
                .uri("/api/v1/care/requests/sent?status=ACCEPTED")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].wardKey").isEqualTo(ward.getMemberKey())
                .jsonPath("$.data[0].wardName").isEqualTo(ward.getName());
    }

    @Test
    @DisplayName("GET /api/v1/care/requests/sent - 관리자 추가 요청은 대기 중에도 대상자 키·이름을 내려준다")
    void getSentRequests_reveals_ward_of_pending_manager_request() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careInvitationRepository.save(
                CareFixture.createManagerInvitation(guardian.getMemberKey(), manager.getMemberKey(), ward.getMemberKey()));

        client.get()
                .uri("/api/v1/care/requests/sent")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].wardKey").isEqualTo(ward.getMemberKey())
                .jsonPath("$.data[0].wardName").isEqualTo(ward.getName());
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/{requestKey}/resend - 대기 중인 요청을 재전송하면 성공하고 요청은 대기 상태로 남으며 5분 재전송 간격이 시작된다")
    void resendRequest_succeeds_and_starts_interval_for_pending_request() {
        CareInvitation pending = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/" + pending.getRequestKey() + "/resend")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(statusOf(pending)).isEqualTo(CareInvitationStatus.PENDING);
        assertThat(redisTemplate.getExpire("care:invitation:resend:" + pending.getRequestKey(), TimeUnit.SECONDS))
                .isBetween(290L, 300L);
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/{requestKey}/resend - 재전송 직후 다시 재전송하면 429와 5분 이내의 남은 초를 받는다")
    void resendRequest_rejects_second_resend_within_interval() {
        CareInvitation pending = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));
        client.post()
                .uri("/api/v1/care/requests/" + pending.getRequestKey() + "/resend")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/api/v1/care/requests/" + pending.getRequestKey() + "/resend")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5019")
                .jsonPath("$.error.data.seconds").value(seconds ->
                        assertThat(((Number) seconds).longValue()).isBetween(1L, 300L));
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/{requestKey}/resend - 다른 사람이 보낸 요청은 재전송할 수 없다")
    void resendRequest_fails_for_request_sent_by_someone_else() {
        Member other = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        CareInvitation pending = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/" + pending.getRequestKey() + "/resend")
                .header(HttpHeaders.AUTHORIZATION, authHeader(other))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5003");
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/{requestKey}/resend - 이미 수락된 요청은 재전송할 수 없다")
    void resendRequest_fails_for_accepted_request() {
        CareInvitation accepted = careInvitationRepository.save(
                CareFixture.createAcceptedWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/" + accepted.getRequestKey() + "/resend")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5006");
    }

    @Test
    @DisplayName("DELETE /api/v1/care/requests/{requestKey} - 대기 중인 요청을 취소하면 요청 행이 삭제된다")
    void cancelRequest_deletes_pending_request() {
        CareInvitation pending = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/requests/" + pending.getRequestKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(careInvitationRepository.findByRequestKey(pending.getRequestKey())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/requests/{requestKey} - 다른 사람이 보낸 요청은 취소할 수 없고 요청은 남는다")
    void cancelRequest_fails_for_request_sent_by_someone_else() {
        Member other = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        CareInvitation pending = careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/requests/" + pending.getRequestKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(other))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5003");

        assertThat(statusOf(pending)).isEqualTo(CareInvitationStatus.PENDING);
    }

    @Test
    @DisplayName("DELETE /api/v1/care/requests/{requestKey} - 이미 수락된 요청은 취소할 수 없고 요청은 남는다")
    void cancelRequest_fails_for_accepted_request() {
        CareInvitation accepted = careInvitationRepository.save(
                CareFixture.createAcceptedWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/requests/" + accepted.getRequestKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5006");

        assertThat(statusOf(accepted)).isEqualTo(CareInvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 같은 대상자에게 대기 중인 요청이 있으면 E5009이고 요청이 늘지 않는다")
    void requestAddWard_fails_when_pending_request_exists() {
        careInvitationRepository.save(
                CareFixture.createWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5009");

        assertThat(careInvitationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/manager - 같은 사람에게 같은 대상자로 대기 중인 관리자 요청이 있으면 E5009이고 요청이 늘지 않는다")
    void requestAddManager_fails_when_pending_request_exists() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careInvitationRepository.save(
                CareFixture.createManagerInvitation(guardian.getMemberKey(), manager.getMemberKey(), ward.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/manager")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s", "wardMemberKey": "%s"}
                        """.formatted(CareFixture.MANAGER_PHONE, ward.getMemberKey()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5009");

        assertThat(careInvitationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/guardian - 나중에 합류한 보호자도 신규 보호자를 초대할 수 있다")
    void requestAddGuardian_succeeds_from_later_joined_guardian() {
        Member laterGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        Member invitee = memberRepository.save(CareFixture.createGuardianMember("01099990000"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), laterGuardian.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/guardian")
                .header(HttpHeaders.AUTHORIZATION, authHeader(laterGuardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "01099990000", "wardMemberKey": "%s"}
                        """.formatted(ward.getMemberKey()))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findAll())
                .singleElement()
                .satisfies(invitation -> {
                    assertThat(invitation.getRequesterMemberKey()).isEqualTo(laterGuardian.getMemberKey());
                    assertThat(invitation.getTargetMemberKey()).isEqualTo(invitee.getMemberKey());
                    assertThat(invitation.getWardMemberKey()).isEqualTo(ward.getMemberKey());
                    assertThat(invitation.getCareRole()).isEqualTo(CareRole.GUARDIAN);
                });
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/guardian - 해당 대상자의 보호자가 아니면 403 E5002이고 초대가 생기지 않는다")
    void requestAddGuardian_fails_when_requester_is_not_guardian_of_ward() {
        Member stranger = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        memberRepository.save(CareFixture.createGuardianMember("01099990000"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/guardian")
                .header(HttpHeaders.AUTHORIZATION, authHeader(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "01099990000", "wardMemberKey": "%s"}
                        """.formatted(ward.getMemberKey()))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(careInvitationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/care/requests/ward - 이전 요청이 거절됐으면 같은 대상자에게 다시 보낼 수 있다")
    void requestAddWard_allows_resend_after_rejection() {
        careInvitationRepository.save(
                CareFixture.createRejectedWardInvitation(guardian.getMemberKey(), ward.getMemberKey()));

        client.post()
                .uri("/api/v1/care/requests/ward")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phoneNumber": "%s"}
                        """.formatted(CareFixture.WARD_PHONE))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findAll())
                .extracting(CareInvitation::getStatus)
                .containsExactlyInAnyOrder(CareInvitationStatus.REJECTED, CareInvitationStatus.PENDING);
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

    @Test
    @DisplayName("GET /api/v1/care/wards - 대상자 가입 시각을 wardJoinedAt으로 내려준다")
    void getMyWards_returns_ward_joined_at() {
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        overwriteMemberCreatedAt(ward, CareFixture.WARD_JOINED_AT);

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].wardMemberKey").isEqualTo(ward.getMemberKey())
                .jsonPath("$.data[0].wardJoinedAt").isEqualTo(CareFixture.WARD_JOINED_AT_TEXT);
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
    @DisplayName("GET /api/v1/care/wards/{wardKey}/caregivers - 나중에 합류한 보호자도 조회할 수 있다")
    void getCaregivers_success_as_co_guardian() {
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 떠나는 사람이 보낸 대기 중 초대는 케어 관계가 남아 있어도 사라진다")
    void removeWard_clears_pending_invitations_sent_by_the_leaver() {
        Member remainingGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member invitee = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        Member otherInvitee = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), remainingGuardian.getMemberKey()));
        CareInvitation sentByLeaver = careInvitationRepository.save(CareFixture.createManagerInvitation(
                guardian.getMemberKey(), invitee.getMemberKey(), ward.getMemberKey()));
        CareInvitation sentByRemaining = careInvitationRepository.save(CareFixture.createManagerInvitation(
                remainingGuardian.getMemberKey(), otherInvitee.getMemberKey(), ward.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findByRequestKey(sentByLeaver.getRequestKey())).isEmpty();
        assertThat(careInvitationRepository.findByRequestKey(sentByRemaining.getRequestKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 떠나는 사람이 보낸 초대라도 이미 수락된 것은 남는다")
    void removeWard_keeps_already_accepted_invitations_sent_by_the_leaver() {
        Member remainingGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member accepted = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), remainingGuardian.getMemberKey()));
        CareInvitation acceptedInvitation = CareFixture.createManagerInvitation(
                guardian.getMemberKey(), accepted.getMemberKey(), ward.getMemberKey());
        acceptedInvitation.accept();
        careInvitationRepository.save(acceptedInvitation);

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findByRequestKey(acceptedInvitation.getRequestKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 떠나는 사람이 다른 보호 대상자에게 보낸 초대는 남는다")
    void removeWard_keeps_invitations_the_leaver_sent_for_other_wards() {
        Member otherWard = memberRepository.save(CareFixture.createWardMember("01088889999"));
        Member remainingGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        Member invitee = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), remainingGuardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(otherWard.getMemberKey(), guardian.getMemberKey()));
        CareInvitation otherWardInvitation = careInvitationRepository.save(CareFixture.createManagerInvitation(
                guardian.getMemberKey(), invitee.getMemberKey(), otherWard.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careInvitationRepository.findByRequestKey(otherWardInvitation.getRequestKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 보호자가 이탈하면 자기 관계만 빠지고 남은 보호자의 역할은 GUARDIAN 그대로다")
    void removeWard_deletes_only_own_relationship_and_keeps_remaining_guardian_role() {
        Member coGuardian = memberRepository.save(CareFixture.createGuardianMember("01066665555"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), coGuardian.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(coGuardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRelationshipRepository.findCareRelationship(
                ward.getMemberKey(), coGuardian.getMemberKey())).isEmpty();
        assertThat(careRoleOf(guardian)).isEqualTo(CareRole.GUARDIAN);
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 마지막 보호자가 이탈해도 다른 보호자가 대상자에게 보낸 대기 요청은 남는다")
    void removeWard_keeps_ward_requests_from_other_guardians_when_last_guardian_leaves() {
        Member otherRequester = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        CareInvitation otherRequest = careInvitationRepository.save(
                CareFixture.createWardInvitation(otherRequester.getMemberKey(), ward.getMemberKey()));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(careRelationshipRepository.findAllByWardMemberKey(ward.getMemberKey())).isEmpty();
        assertThat(statusOf(otherRequest)).isEqualTo(CareInvitationStatus.PENDING);
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 이탈한 보호자의 알림 설정·위치 주기만 지워지고 남은 보호자의 설정은 그대로다")
    void removeWard_deletes_settings_of_leaving_guardian_only() {
        Member remainingGuardian = memberRepository.save(CareFixture.createGuardianMember(CareFixture.OTHER_GUARDIAN_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), remainingGuardian.getMemberKey()));
        notificationSettingRepository.save(
                NotificationFixture.createSetting(ward.getMemberKey(), guardian.getMemberKey()));
        notificationSettingRepository.save(
                NotificationFixture.createSetting(ward.getMemberKey(), remainingGuardian.getMemberKey()));
        locationSettingRepository.save(
                LocationFixture.createLocationSetting(ward.getMemberKey(), guardian.getMemberKey(), 180));
        locationSettingRepository.save(
                LocationFixture.createLocationSetting(ward.getMemberKey(), remainingGuardian.getMemberKey(), 60));

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(notificationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(
                ward.getMemberKey(), guardian.getMemberKey())).isEmpty();
        assertThat(locationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(
                ward.getMemberKey(), guardian.getMemberKey())).isEmpty();
        assertThat(notificationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(
                ward.getMemberKey(), remainingGuardian.getMemberKey())).isPresent();
        assertThat(locationSettingRepository.findByWardMemberKeyAndCaregiverMemberKey(
                ward.getMemberKey(), remainingGuardian.getMemberKey())).isPresent();
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

    // ── 보호 대상자 별명 수정 ──────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/nickname - 별명을 설정하면 내 관계 행에만 저장되고 다른 보호자 행은 그대로다")
    void updateWardNickname_saves_only_on_requester_relationship() {
        Member otherGuardian = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

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
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        patchNickname(guardian, "할머니");

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].wardName").isEqualTo("보호대상자")
                .jsonPath("$.data[0].wardNickname").isEqualTo("할머니")
                .jsonPath("$.data[0].myRole").isEqualTo("GUARDIAN");
    }

    // ── 케어 인원 한도 ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/care/requests/manager - 대상자에 연결된 사람이 이미 5명이면 400 E5000이고 초대가 생기지 않는다")
    void sendManagerInvitation_fails_when_ward_is_full() {
        memberRepository.save(CareFixture.createGuardianMember("01099990000"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
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

    private void overwriteCreatedAt(CareInvitation invitation, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE care_invitation SET created_at = ? WHERE care_invitation_id = ?",
                createdAt, invitation.getId()
        );
    }

    private void overwriteMemberCreatedAt(Member member, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE members SET created_at = ? WHERE member_id = ?",
                createdAt, member.getId()
        );
    }

    private void connectGuardians(Member targetWard, String... phones) {
        for (String phone : phones) {
            Member caregiver = memberRepository.save(CareFixture.createGuardianMember(phone));
            careRelationshipRepository.save(
                    CareFixture.createGuardianRelationship(targetWard.getMemberKey(), caregiver.getMemberKey()));
        }
    }

    private CareInvitationStatus statusOf(CareInvitation invitation) {
        return careInvitationRepository.findByRequestKey(invitation.getRequestKey())
                .orElseThrow(() -> new AssertionError("케어 요청이 존재하지 않는다"))
                .getStatus();
    }

    // ── 프로필 아바타 지정 ────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/avatar - 지정한 얼굴은 내 대상자 목록에만 보이고 다른 보호자 목록에는 null이다")
    void designateWardAvatar_is_visible_only_to_requester() {
        Member otherGuardian = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), otherGuardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(CareFixture.SENIOR_AVATAR_CODE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].designatedProfileAvatarCode").isEqualTo(CareFixture.SENIOR_AVATAR_CODE);

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, authHeader(otherGuardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].designatedProfileAvatarCode").isEqualTo(null);
    }

    @Test
    @DisplayName("GET /api/v1/care/wards - 대상자 본인이 고른 얼굴과 내가 지정한 얼굴은 별개 필드로 함께 내려온다")
    void getMyWards_returns_own_and_designated_avatar_separately() {
        ward.changeProfileAvatarCode(CareFixture.SENIOR_AVATAR_CODE);
        memberRepository.save(ward);
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        patchWardAvatar(guardian, CareFixture.OTHER_SENIOR_AVATAR_CODE);

        client.get()
                .uri("/api/v1/care/wards")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].wardProfileAvatarCode").isEqualTo(CareFixture.SENIOR_AVATAR_CODE)
                .jsonPath("$.data[0].designatedProfileAvatarCode").isEqualTo(CareFixture.OTHER_SENIOR_AVATAR_CODE);

        assertThat(memberRepository.findByMemberKey(ward.getMemberKey()).orElseThrow().getProfileAvatarCode())
                .isEqualTo(CareFixture.SENIOR_AVATAR_CODE);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/avatar - 빈 문자열을 보내면 지정 행이 삭제된다")
    void designateWardAvatar_clears_designation_when_blank() {
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        patchWardAvatar(guardian, CareFixture.SENIOR_AVATAR_CODE);

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": ""}
                        """)
                .exchange()
                .expectStatus().isOk();

        assertThat(designatedAvatarRepository.findAllByOwnerMemberKey(guardian.getMemberKey())).isEmpty();
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/avatar - 허용 코드 외 값이면 400 E3008이고 저장되지 않는다")
    void designateWardAvatar_rejects_unknown_code() {
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(CareFixture.UNKNOWN_AVATAR_CODE))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E3008");

        assertThat(designatedAvatarRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/avatar - 케어 관계가 없으면 400 E5011이고 저장되지 않는다")
    void designateWardAvatar_fails_when_relationship_not_found() {
        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(CareFixture.SENIOR_AVATAR_CODE))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");

        assertThat(designatedAvatarRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/avatar - 지정한 얼굴은 내 보호자 목록에만 보이고 다른 보호자 목록에는 null이다")
    void designateCaregiverAvatar_is_visible_only_to_requester() {
        Member otherGuardian = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        manager.changeProfileAvatarCode(CareFixture.ADULT_AVATAR_CODE);
        memberRepository.save(manager);
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), otherGuardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + manager.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(CareFixture.SENIOR_AVATAR_CODE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        String managerFilter = "$.data[?(@.memberKey == '" + manager.getMemberKey() + "')]";
        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath(managerFilter + ".profileAvatarCode").isEqualTo(CareFixture.ADULT_AVATAR_CODE)
                .jsonPath(managerFilter + ".designatedProfileAvatarCode").isEqualTo(CareFixture.SENIOR_AVATAR_CODE);

        client.get()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers")
                .header(HttpHeaders.AUTHORIZATION, authHeader(otherGuardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath(managerFilter + ".profileAvatarCode").isEqualTo(CareFixture.ADULT_AVATAR_CODE)
                .jsonPath(managerFilter + ".designatedProfileAvatarCode").isEqualTo(null);
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/avatar - 관계자가 요청하면 403 E5002이고 저장되지 않는다")
    void designateCaregiverAvatar_forbidden_for_manager() {
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + guardian.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(manager))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(CareFixture.ADULT_AVATAR_CODE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5002");

        assertThat(designatedAvatarRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("PATCH /api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/avatar - 대상이 케어 관계에 없으면 400 E5011이고 저장되지 않는다")
    void designateCaregiverAvatar_fails_when_target_not_in_care() {
        Member stranger = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));

        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + stranger.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(CareFixture.ADULT_AVATAR_CODE))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E5011");

        assertThat(designatedAvatarRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/care/wards/{wardKey} - 케어 관계에서 나가면 내가 그 대상자 범위에 지정한 얼굴이 모두 삭제된다")
    void removeWard_deletes_designations_made_by_the_leaving_member() {
        Member otherGuardian = memberRepository.save(CareFixture.createGuardianMember("01077778888"));
        Member manager = memberRepository.save(CareFixture.createGuardianMember(CareFixture.MANAGER_PHONE));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), guardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createGuardianRelationship(ward.getMemberKey(), otherGuardian.getMemberKey()));
        careRelationshipRepository.save(
                CareFixture.createManagerRelationship(ward.getMemberKey(), manager.getMemberKey()));
        patchWardAvatar(guardian, CareFixture.SENIOR_AVATAR_CODE);
        patchCaregiverAvatar(guardian, manager, CareFixture.ADULT_AVATAR_CODE);
        patchWardAvatar(otherGuardian, CareFixture.OTHER_SENIOR_AVATAR_CODE);

        client.delete()
                .uri("/api/v1/care/wards/" + ward.getMemberKey())
                .header(HttpHeaders.AUTHORIZATION, authHeader(guardian))
                .exchange()
                .expectStatus().isOk();

        assertThat(designatedAvatarRepository.findAllByOwnerMemberKey(guardian.getMemberKey())).isEmpty();
        assertThat(designatedAvatarRepository.findAllByOwnerMemberKey(otherGuardian.getMemberKey())).hasSize(1);
    }

    private void patchWardAvatar(Member caregiver, String code) {
        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(caregiver))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(code))
                .exchange()
                .expectStatus().isOk();
    }

    private void patchCaregiverAvatar(Member requester, Member target, String code) {
        client.patch()
                .uri("/api/v1/care/wards/" + ward.getMemberKey() + "/caregivers/" + target.getMemberKey() + "/avatar")
                .header(HttpHeaders.AUTHORIZATION, authHeader(requester))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"profileAvatarCode": "%s"}
                        """.formatted(code))
                .exchange()
                .expectStatus().isOk();
    }
}
