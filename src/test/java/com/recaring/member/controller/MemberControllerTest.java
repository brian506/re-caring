package com.recaring.member.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.dataaccess.repository.MemberWithdrawalRepository;
import com.recaring.member.dataaccess.repository.MembersTermsAgreementRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.TokenPayload;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("MemberController HTTP 통합 테스트")
class MemberControllerTest extends AbstractIntegrationTest {

    private static final String GUARDIAN_EMAIL = "guardian@test.com";
    private static final String WARD_EMAIL = "ward@test.com";
    private static final String PASSWORD = MemberFixture.CURRENT_PASSWORD;
    private static final String NAME_20_CHARS = "가나다라마바사아자차카타파하가나다라마바";
    private static final String NAME_21_CHARS = NAME_20_CHARS + "사";

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private LocalAuthRepository localAuthRepository;
    @Autowired
    private MembersTermsAgreementRepository membersTermsAgreementRepository;
    @Autowired
    private MemberWithdrawalRepository memberWithdrawalRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtGenerator jwtGenerator;

    private Member guardian;
    private Member ward;

    @BeforeEach
    void setUp() {
        guardian = memberRepository.save(MemberFixture.createMember(MemberFixture.PHONE));
        ward = memberRepository.save(MemberFixture.createWardMember(MemberFixture.OTHER_PHONE));

        String encoded = passwordEncoder.encode(PASSWORD);
        localAuthRepository.save(LocalAuth.of(guardian.getMemberKey(), GUARDIAN_EMAIL, encoded));
        localAuthRepository.save(LocalAuth.of(ward.getMemberKey(), WARD_EMAIL, encoded));
        membersTermsAgreementRepository.save(MemberFixture.createTermsAgreement(guardian.getMemberKey()));
        membersTermsAgreementRepository.save(MemberFixture.createTermsAgreement(ward.getMemberKey()));
    }

    @AfterEach
    void tearDown() {
        memberWithdrawalRepository.deleteAllInBatch();
        membersTermsAgreementRepository.deleteAllInBatch();
        localAuthRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    private String bearerToken(Member member) {
        return "Bearer " + jwtGenerator.generateJwt(
                new TokenPayload(member.getMemberKey(), member.getRole(), new Date())
        ).accessToken();
    }

    private String storedPassword(Member member) {
        return localAuthRepository.findByMemberKey(member.getMemberKey()).orElseThrow().getPassword();
    }

    @Test
    @DisplayName("GET /me - 로그인한 회원 본인의 정보·이메일·약관 동의 시각이 반환된다")
    void getMyInfo_returns_own_profile_email_and_terms() {
        client.get()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.memberKey").isEqualTo(guardian.getMemberKey())
                .jsonPath("$.data.name").isEqualTo(MemberFixture.NAME)
                .jsonPath("$.data.phone").isEqualTo(MemberFixture.PHONE)
                .jsonPath("$.data.role").isEqualTo(MemberRole.GUARDIAN.name())
                .jsonPath("$.data.email").isEqualTo(GUARDIAN_EMAIL)
                .jsonPath("$.data.gender").isEqualTo("남")
                .jsonPath("$.data.termsServiceAgreedAt").isNotEmpty();
    }

    @Test
    @DisplayName("GET /me - 인증 없이 요청하면 401이 반환된다")
    void getMyInfo_requires_authentication() {
        client.get()
                .uri("/api/v1/members/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("PATCH /me - 이름과 생년월일을 보내면 DB에 반영된다")
    void updateMyInfo_persists_name_and_birth() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "%s", "birth": "%s"}
                        """.formatted(MemberFixture.UPDATED_NAME, MemberFixture.UPDATED_BIRTH))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        Member updated = memberRepository.findByMemberKey(guardian.getMemberKey()).orElseThrow();
        assertThat(updated.getName()).isEqualTo(MemberFixture.UPDATED_NAME);
        assertThat(updated.getBirth()).isEqualTo(MemberFixture.UPDATED_BIRTH);
    }

    @Test
    @DisplayName("PATCH /me - 이름만 보내면 생년월일은 그대로 유지된다")
    void updateMyInfo_keeps_birth_when_only_name_sent() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "%s"}
                        """.formatted(MemberFixture.UPDATED_NAME))
                .exchange()
                .expectStatus().isOk();

        Member updated = memberRepository.findByMemberKey(guardian.getMemberKey()).orElseThrow();
        assertThat(updated.getName()).isEqualTo(MemberFixture.UPDATED_NAME);
        assertThat(updated.getBirth()).isEqualTo(MemberFixture.BIRTH);
    }

    @Test
    @DisplayName("PATCH /me - 이름이 정확히 20자면 허용된다")
    void updateMyInfo_accepts_name_of_exactly_20_chars() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "%s"}
                        """.formatted(NAME_20_CHARS))
                .exchange()
                .expectStatus().isOk();

        assertThat(memberRepository.findByMemberKey(guardian.getMemberKey()).orElseThrow().getName())
                .isEqualTo(NAME_20_CHARS);
    }

    @Test
    @DisplayName("PATCH /me - 이름이 20자를 넘으면 400 E400이 반환되고 이름은 바뀌지 않는다")
    void updateMyInfo_rejects_name_longer_than_20_chars() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "%s"}
                        """.formatted(NAME_21_CHARS))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E400");

        assertThat(memberRepository.findByMemberKey(guardian.getMemberKey()).orElseThrow().getName())
                .isEqualTo(MemberFixture.NAME);
    }

    @Test
    @DisplayName("PATCH /me - 생년월일이 오늘이면 400 E400이 반환된다")
    void updateMyInfo_rejects_birth_of_today() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"birth": "%s"}
                        """.formatted(LocalDate.now()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E400");
    }

    @Test
    @DisplayName("PATCH /me - 생년월일이 오늘 이후면 400 E400이 반환된다")
    void updateMyInfo_rejects_future_birth() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"birth": "%s"}
                        """.formatted(LocalDate.now().plusDays(1)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E400");
    }

    @Test
    @DisplayName("PATCH /me - 현재 비밀번호가 맞으면 저장된 비밀번호가 새 값으로 교체된다")
    void updateMyInfo_replaces_password_when_current_password_matches() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(PASSWORD, MemberFixture.NEW_PASSWORD))
                .exchange()
                .expectStatus().isOk();

        String stored = storedPassword(guardian);
        assertThat(passwordEncoder.matches(MemberFixture.NEW_PASSWORD, stored)).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, stored)).isFalse();
    }

    @Test
    @DisplayName("PATCH /me - 현재 비밀번호가 틀리면 400 E2017이 반환되고 비밀번호는 바뀌지 않는다")
    void updateMyInfo_rejects_password_change_when_current_password_wrong() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(MemberFixture.WRONG_PASSWORD, MemberFixture.NEW_PASSWORD))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E2017");

        assertThat(passwordEncoder.matches(PASSWORD, storedPassword(guardian))).isTrue();
    }

    @Test
    @DisplayName("PATCH /me - 현재 비밀번호가 틀리면 함께 보낸 이름도 반영되지 않는다")
    void updateMyInfo_rolls_back_name_when_password_verification_fails() {
        client.patch()
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "%s", "currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(MemberFixture.UPDATED_NAME, MemberFixture.WRONG_PASSWORD, MemberFixture.NEW_PASSWORD))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(memberRepository.findByMemberKey(guardian.getMemberKey()).orElseThrow().getName())
                .isEqualTo(MemberFixture.NAME);
    }

    @Test
    @DisplayName("POST /phones - 보호자가 요청하면 가입된 번호의 회원만 memberKey·이름·전화번호·역할로 반환된다")
    void findByPhones_returns_only_registered_members() {
        client.post()
                .uri("/api/v1/members/phones")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phones": ["%s", "%s"]}
                        """.formatted(MemberFixture.PHONE, MemberFixture.UNREGISTERED_PHONE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].memberKey").isEqualTo(guardian.getMemberKey())
                .jsonPath("$.data[0].name").isEqualTo(MemberFixture.NAME)
                .jsonPath("$.data[0].phone").isEqualTo(MemberFixture.PHONE)
                .jsonPath("$.data[0].role").isEqualTo(MemberRole.GUARDIAN.name());
    }

    @Test
    @DisplayName("POST /phones - 보호 대상자가 요청하면 403이 반환된다")
    void findByPhones_is_forbidden_for_ward() {
        client.post()
                .uri("/api/v1/members/phones")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(ward))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phones": ["%s"]}
                        """.formatted(MemberFixture.PHONE))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /phones - 번호 목록이 비어 있으면 400 E400이 반환된다")
    void findByPhones_rejects_empty_phone_list() {
        client.post()
                .uri("/api/v1/members/phones")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"phones": []}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E400");
    }

    @Test
    @DisplayName("DELETE /me - 비밀번호가 맞으면 회원·인증·약관이 삭제되고 탈퇴 이력이 남으며 다른 회원은 유지된다")
    void withdraw_removes_own_data_and_keeps_others() {
        client.method(HttpMethod.DELETE)
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"password": "%s"}
                        """.formatted(PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(memberRepository.findByMemberKey(guardian.getMemberKey())).isEmpty();
        assertThat(localAuthRepository.findByMemberKey(guardian.getMemberKey())).isEmpty();
        assertThat(membersTermsAgreementRepository.findByMemberKey(guardian.getMemberKey())).isEmpty();

        assertThat(memberWithdrawalRepository.findAll())
                .extracting("memberKey", "email")
                .containsExactly(tuple(guardian.getMemberKey(), GUARDIAN_EMAIL));

        assertThat(memberRepository.findByMemberKey(ward.getMemberKey())).isPresent();
        assertThat(localAuthRepository.findByMemberKey(ward.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("DELETE /me - 비밀번호가 틀리면 400 E2017이 반환되고 회원은 남아 있다")
    void withdraw_rejects_wrong_password() {
        client.method(HttpMethod.DELETE)
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"password": "%s"}
                        """.formatted(MemberFixture.WRONG_PASSWORD))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E2017");

        assertThat(memberRepository.findByMemberKey(guardian.getMemberKey())).isPresent();
        assertThat(memberWithdrawalRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("DELETE /me - 비밀번호가 비어 있으면 400 E400이 반환된다")
    void withdraw_rejects_blank_password() {
        client.method(HttpMethod.DELETE)
                .uri("/api/v1/members/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(guardian))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"password": ""}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E400");

        assertThat(memberRepository.findByMemberKey(guardian.getMemberKey())).isPresent();
    }
}
