package com.recaring.auth.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.dataaccess.repository.RefreshTokenRepository;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.dataaccess.repository.MembersTermsAgreementRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.Jwt;
import com.recaring.security.vo.TokenPayload;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController HTTP 통합 테스트")
class AuthControllerTest extends AbstractIntegrationTest {

    private static final String TOKEN_KEY_PREFIX = "phone:token:";
    private static final long REFRESH_EXPIRATION_MS = 1_209_600_000L;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LocalAuthRepository localAuthRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MembersTermsAgreementRepository membersTermsAgreementRepository;

    @Autowired
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtGenerator jwtGenerator;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        membersTermsAgreementRepository.deleteAll();
        localAuthRepository.deleteAll();
        memberRepository.deleteAll();
        fcmDeviceTokenRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    private String prepareVerificationToken(String phone) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, phone, 10, TimeUnit.MINUTES);
        return token;
    }

    private Member prepareLocalMember(String phone, String email, String rawPassword) {
        Member member = memberRepository.save(MemberFixture.createMember(phone));
        localAuthRepository.save(LocalAuth.builder()
                .memberKey(member.getMemberKey())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build());
        return member;
    }

    private String prepareStoredRefreshToken(Member member) {
        Jwt jwt = jwtGenerator.generateJwt(
                new TokenPayload(member.getMemberKey(), member.getRole(), new Date())
        );
        refreshTokenRepository.save(
                RefreshToken.of(member.getMemberKey(), jwt.refreshToken(), REFRESH_EXPIRATION_MS)
        );
        return jwt.refreshToken();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up - 회원가입에 성공하면 회원·로컬 인증·약관 동의가 함께 저장된다")
    void signUp_success() {
        String verificationToken = prepareVerificationToken(SmsFixture.PHONE);

        client.post()
                .uri("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "verificationToken": "%s",
                            "email": "newuser@example.com",
                            "password": "%s",
                            "name": "%s",
                            "birth": "1990-01-01",
                            "gender": "MALE",
                            "role": "GUARDIAN",
                            "isTermsOfServiceAgreed": true,
                            "isLocationServiceAgreed": true,
                            "isPrivacyPolicyAgreed": true
                        }
                        """.formatted(verificationToken, AuthFixture.RAW_PASSWORD, MemberFixture.NAME))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        Member saved = memberRepository.findByPhone(SmsFixture.PHONE).orElseThrow();
        assertThat(saved.getName()).isEqualTo(MemberFixture.NAME);

        LocalAuth localAuth = localAuthRepository.findByEmail("newuser@example.com").orElseThrow();
        assertThat(localAuth.getMemberKey()).isEqualTo(saved.getMemberKey());
        assertThat(localAuth.getPassword()).isNotEqualTo(AuthFixture.RAW_PASSWORD);
        assertThat(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, localAuth.getPassword())).isTrue();

        assertThat(membersTermsAgreementRepository.findByMemberKey(saved.getMemberKey())).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up - 인증되지 않은 전화번호면 아무것도 저장하지 않는다")
    void signUp_fail_when_phone_not_verified() {
        String invalidToken = UUID.randomUUID().toString();

        client.post()
                .uri("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "verificationToken": "%s",
                            "email": "fail@example.com",
                            "password": "%s",
                            "name": "홍길동",
                            "birth": "1990-01-01",
                            "gender": "MALE",
                            "role": "GUARDIAN",
                            "isTermsOfServiceAgreed": true,
                            "isLocationServiceAgreed": true,
                            "isPrivacyPolicyAgreed": true
                        }
                        """.formatted(invalidToken, AuthFixture.RAW_PASSWORD))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E4002");

        assertThat(localAuthRepository.findByEmail("fail@example.com")).isEmpty();
        assertThat(memberRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up - 이미 가입된 이메일이면 회원이 추가되지 않는다")
    void signUp_fail_when_email_already_registered() {
        prepareLocalMember(MemberFixture.OTHER_PHONE, "duplicate@example.com", AuthFixture.RAW_PASSWORD);
        String verificationToken = prepareVerificationToken(SmsFixture.PHONE);

        client.post()
                .uri("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "verificationToken": "%s",
                            "email": "duplicate@example.com",
                            "password": "%s",
                            "name": "홍길동",
                            "birth": "1990-01-01",
                            "gender": "MALE",
                            "role": "GUARDIAN",
                            "isTermsOfServiceAgreed": true,
                            "isLocationServiceAgreed": true,
                            "isPrivacyPolicyAgreed": true
                        }
                        """.formatted(verificationToken, AuthFixture.RAW_PASSWORD))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E3002");

        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(memberRepository.findByPhone(SmsFixture.PHONE)).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in/local - 로그인에 성공하면 Access Token은 바디로, Refresh Token은 쿠키와 DB에 남는다")
    void signIn_success() {
        Member member = prepareLocalMember(SmsFixture.PHONE, "login@example.com", AuthFixture.RAW_PASSWORD);

        client.post()
                .uri("/api/v1/auth/sign-in/local")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "login@example.com",
                            "password": "%s"
                        }
                        """.formatted(AuthFixture.RAW_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.accessToken").isNotEmpty();

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        RefreshToken issued = refreshTokenRepository.findAll().getFirst();
        assertThat(issued.getMemberKey()).isEqualTo(member.getMemberKey());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in/local - 비밀번호가 틀리면 토큰을 발급하지 않는다")
    void signIn_fail_with_wrong_password() {
        prepareLocalMember(MemberFixture.OTHER_PHONE, "user@example.com", AuthFixture.RAW_PASSWORD);

        client.post()
                .uri("/api/v1/auth/sign-in/local")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "user@example.com",
                            "password": "wrongPass1"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E2017");

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - 저장된 리프레시 토큰으로 갱신하면 기존 토큰이 폐기되고 새 토큰이 저장된다")
    void refresh_success() {
        Member member = prepareLocalMember(SmsFixture.PHONE, "refresh@example.com", AuthFixture.RAW_PASSWORD);
        String oldRefreshToken = prepareStoredRefreshToken(member);

        client.post()
                .uri("/api/v1/auth/refresh")
                .header(HttpHeaders.COOKIE, "refresh_token=" + oldRefreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.accessToken").isNotEmpty();

        assertThat(refreshTokenRepository.findByToken(oldRefreshToken)).isEmpty();
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.findAll().getFirst().getMemberKey())
                .isEqualTo(member.getMemberKey());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - DB에 없는 리프레시 토큰이면 갱신되지 않는다")
    void refresh_fail_when_token_not_stored() {
        Member member = prepareLocalMember(SmsFixture.PHONE, "refresh@example.com", AuthFixture.RAW_PASSWORD);
        String unknownToken = jwtGenerator.generateJwt(
                new TokenPayload(member.getMemberKey(), member.getRole(), new Date())
        ).refreshToken();

        client.post()
                .uri("/api/v1/auth/refresh")
                .header(HttpHeaders.COOKIE, "refresh_token=" + unknownToken)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E2021");

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - 쿠키가 없으면 REQUIRED_AUTH로 거절한다")
    void refresh_fail_when_cookie_missing() {
        client.post()
                .uri("/api/v1/auth/refresh")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E401");
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-out - 로그아웃하면 저장된 리프레시 토큰이 삭제된다")
    void signOut_success() {
        Member member = prepareLocalMember(SmsFixture.PHONE, "signout@example.com", AuthFixture.RAW_PASSWORD);
        String refreshToken = prepareStoredRefreshToken(member);

        client.post()
                .uri("/api/v1/auth/sign-out")
                .header(HttpHeaders.COOKIE, "refresh_token=" + refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-out - fcmToken을 함께 보내면 해당 기기의 FCM 토큰도 삭제된다")
    void signOut_deletes_fcmToken_when_provided() {
        Member member = prepareLocalMember(SmsFixture.PHONE, "signout@example.com", AuthFixture.RAW_PASSWORD);
        String refreshToken = prepareStoredRefreshToken(member);
        fcmDeviceTokenRepository.save(
                NotificationFixture.guardianFcmDeviceToken(NotificationFixture.GUARDIAN_FCM_TOKEN));

        client.post()
                .uri("/api/v1/auth/sign-out")
                .header(HttpHeaders.COOKIE, "refresh_token=" + refreshToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "fcmToken": "%s"
                        }
                        """.formatted(NotificationFixture.GUARDIAN_FCM_TOKEN))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        assertThat(fcmDeviceTokenRepository.findByToken(NotificationFixture.GUARDIAN_FCM_TOKEN)).isEmpty();
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-out - 쿠키가 없으면 REQUIRED_AUTH로 거절한다")
    void signOut_fail_when_cookie_missing() {
        client.post()
                .uri("/api/v1/auth/sign-out")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E401");
    }

    @Test
    @DisplayName("POST /api/v1/auth/password - 인증된 전화번호의 계정 비밀번호가 새 값으로 교체된다")
    void resetPassword_success() {
        Member member = prepareLocalMember(SmsFixture.PHONE, "reset@example.com", AuthFixture.RAW_PASSWORD);
        String smsToken = prepareVerificationToken(SmsFixture.PHONE);

        client.post()
                .uri("/api/v1/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "smsToken": "%s",
                            "password": "%s"
                        }
                        """.formatted(smsToken, MemberFixture.NEW_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        LocalAuth updated = localAuthRepository.findByMemberKey(member.getMemberKey()).orElseThrow();
        assertThat(passwordEncoder.matches(MemberFixture.NEW_PASSWORD, updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, updated.getPassword())).isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/auth/password - 인증되지 않은 smsToken이면 비밀번호가 그대로 유지된다")
    void resetPassword_fail_when_token_not_verified() {
        Member member = prepareLocalMember(SmsFixture.PHONE, "reset@example.com", AuthFixture.RAW_PASSWORD);

        client.post()
                .uri("/api/v1/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "smsToken": "%s",
                            "password": "%s"
                        }
                        """.formatted(UUID.randomUUID(), MemberFixture.NEW_PASSWORD))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E4002");

        LocalAuth unchanged = localAuthRepository.findByMemberKey(member.getMemberKey()).orElseThrow();
        assertThat(passwordEncoder.matches(AuthFixture.RAW_PASSWORD, unchanged.getPassword())).isTrue();
    }

    @Test
    @DisplayName("GET /api/v1/auth/email - 이름/생년월일/전화번호로 마스킹된 이메일을 조회한다")
    void findEmail_success() {
        Member member = memberRepository.save(MemberFixture.createMember(
                "01099998888", "김검색", LocalDate.of(1995, 5, 5), MemberFixture.GENDER));
        localAuthRepository.save(LocalAuth.builder()
                .memberKey(member.getMemberKey())
                .email("findme@example.com")
                .password(AuthFixture.ENCODED_PASSWORD)
                .build());

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/auth/email")
                        .queryParam("name", "김검색")
                        .queryParam("birth", "1995-05-05")
                        .queryParam("phone", "01099998888")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS")
                .jsonPath("$.data.email").isEqualTo("fin****@example.com");
    }
}
