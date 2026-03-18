package com.recaring.auth.controller;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.domain.member.dataaccess.repository.MemberRepository;
import com.recaring.domain.member.fixture.MemberFixture;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController HTTP 통합 테스트")
class AuthControllerTest extends AbstractIntegrationTest {

    private static final String TOKEN_KEY_PREFIX = "phone:token:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:token:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LocalAuthRepository localAuthRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        localAuthRepository.deleteAll();
        memberRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    /**
     * 전화번호 인증이 완료된 상태를 Redis에 직접 세팅 (SMS 발송 과정 생략)
     */
    private String prepareVerificationToken(String phone) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, phone, 10, TimeUnit.MINUTES);
        return token;
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up - 올바른 요청으로 회원가입이 성공한다")
    void signUp_success() {
        String verificationToken = prepareVerificationToken(SmsFixture.PHONE);

        client.post()
                .uri("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
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

        assertThat(localAuthRepository.findByEmail("newuser@example.com")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-up - 인증되지 않은 전화번호로 가입 시 실패한다")
    void signUp_fail_when_phone_not_verified() {
        String invalidToken = UUID.randomUUID().toString();

        client.post()
                .uri("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "verificationToken": "%s",
                            "email": "fail@example.com",
                            "password": "password1",
                            "name": "홍길동",
                            "birth": "1990-01-01",
                            "gender": "MALE",
                            "role": "GUARDIAN",
                            "isTermsOfServiceAgreed": true,
                            "isLocationServiceAgreed": true,
                            "isPrivacyPolicyAgreed": true
                        }
                        """.formatted(invalidToken))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in/local - 올바른 이메일/비밀번호로 로그인이 성공한다")
    void signIn_success() {
        // Member 저장 후 LocalAuth 저장 (이메일/비밀번호 인증을 위해 둘 다 필요)
        String encodedPw = passwordEncoder.encode(AuthFixture.RAW_PASSWORD);
        Member member = MemberFixture.createMember();
        memberRepository.save(member);

        LocalAuth localAuth = LocalAuth.builder()
                .memberKey(member.getMemberKey())
                .email("login@example.com")
                .password(encodedPw)
                .build();
        localAuthRepository.save(localAuth);

        client.post()
                .uri("/api/v1/auth/sign-in/local")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
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
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-in/local - 잘못된 비밀번호로 로그인하면 실패한다")
    void signIn_fail_with_wrong_password() {
        String encodedPw = passwordEncoder.encode(AuthFixture.RAW_PASSWORD);
        Member member = MemberFixture.createMember("01022223333");
        memberRepository.save(member);

        LocalAuth localAuth = LocalAuth.builder()
                .memberKey(member.getMemberKey())
                .email("user@example.com")
                .password(encodedPw)
                .build();
        localAuthRepository.save(localAuth);

        client.post()
                .uri("/api/v1/auth/sign-in/local")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "email": "user@example.com",
                            "password": "wrongPass1"
                        }
                        """)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("POST /api/v1/auth/sign-out - 로그인 후 로그아웃이 성공한다")
    void signOut_success() {
        // 리프레시 토큰을 Redis에 직접 등록
        String refreshToken = UUID.randomUUID().toString();
        String memberKey = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY_PREFIX + refreshToken, memberKey, 14, TimeUnit.DAYS);

        client.post()
                .uri("/api/v1/auth/sign-out")
                .header(HttpHeaders.COOKIE, "refresh_token=" + refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultType").isEqualTo("SUCCESS");

        // 토큰 삭제 검증
        String stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX + refreshToken);
        assertThat(stored).isNull();
    }

    @Test
    @DisplayName("GET /api/v1/auth/email - 이름/생년월일/전화번호로 마스킹된 이메일을 조회한다")
    void findEmail_success() {
        // Member 저장 (phone/name/birth로 조회됨)
        Member member = MemberFixture.createMember("01099998888", "김검색",
                LocalDate.of(1995, 5, 5), MemberFixture.GENDER);
        memberRepository.save(member);

        // LocalAuth 저장 (email 반환을 위해 필요)
        LocalAuth localAuth = LocalAuth.builder()
                .memberKey(member.getMemberKey())
                .email("findme@example.com")
                .password(AuthFixture.ENCODED_PASSWORD)
                .build();
        localAuthRepository.save(localAuth);

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
