package com.recaring.sms.controller;

import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PhoneVerificationController HTTP 통합 테스트")
class PhoneVerificationControllerTest extends AbstractIntegrationTest {

    private static final String CODE_KEY_PREFIX = "phone:verify:";
    private static final String QUOTA_KEY_PREFIX = "sms:quota:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    private void prepareVerificationCode(String phone, String code) {
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone, code, 5, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("POST /api/v1/auth/phone/send-code - 한 시간 한도를 다 쓴 번호는 429와 남은 초를 받고 인증번호가 저장되지 않는다")
    void sendCode_rejects_phone_that_used_up_hourly_quota() {
        redisTemplate.opsForValue().set(QUOTA_KEY_PREFIX + SmsFixture.PHONE, "5", 1800, TimeUnit.SECONDS);

        client.post()
                .uri("/api/v1/auth/phone/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "phone": "%s"
                        }
                        """.formatted(SmsFixture.PHONE))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E4005")
                .jsonPath("$.error.data.seconds").value(seconds ->
                        assertThat(((Number) seconds).longValue()).isBetween(1790L, 1800L));

        assertThat(redisTemplate.hasKey(CODE_KEY_PREFIX + SmsFixture.PHONE)).isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/auth/phone/send-code - 만료 시간이 없는 한도 카운터도 초과 시 1시간 만료가 다시 걸려 영구 차단되지 않는다")
    void sendCode_restores_expiry_on_counter_without_ttl() {
        redisTemplate.opsForValue().set(QUOTA_KEY_PREFIX + SmsFixture.PHONE, "5");

        client.post()
                .uri("/api/v1/auth/phone/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "phone": "%s"
                        }
                        """.formatted(SmsFixture.PHONE))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody()
                .jsonPath("$.error.errorCode").isEqualTo("E4005")
                .jsonPath("$.error.data.seconds").value(seconds ->
                        assertThat(((Number) seconds).longValue()).isBetween(3590L, 3600L));

        Long ttl = redisTemplate.getExpire(QUOTA_KEY_PREFIX + SmsFixture.PHONE, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(3590L, 3600L);
    }

    @Test
    @DisplayName("POST /api/v1/auth/phone/verify - 가입 이력이 없는 번호를 인증하면 registered가 false다")
    void verify_returns_not_registered_for_new_phone() {
        prepareVerificationCode(SmsFixture.PHONE, SmsFixture.CODE);

        client.post()
                .uri("/api/v1/auth/phone/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "phone": "%s",
                            "code": "%s"
                        }
                        """.formatted(SmsFixture.PHONE, SmsFixture.CODE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.verificationToken").isNotEmpty()
                .jsonPath("$.data.registered").isEqualTo(false);
    }

    @Test
    @DisplayName("POST /api/v1/auth/phone/verify - 이미 가입된 번호를 인증하면 registered가 true다")
    void verify_returns_registered_for_existing_phone() {
        memberRepository.save(MemberFixture.createMember(SmsFixture.PHONE));
        prepareVerificationCode(SmsFixture.PHONE, SmsFixture.CODE);

        client.post()
                .uri("/api/v1/auth/phone/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "phone": "%s",
                            "code": "%s"
                        }
                        """.formatted(SmsFixture.PHONE, SmsFixture.CODE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.verificationToken").isNotEmpty()
                .jsonPath("$.data.registered").isEqualTo(true);
    }
}
