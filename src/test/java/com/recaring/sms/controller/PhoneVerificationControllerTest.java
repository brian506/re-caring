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
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

@DisplayName("PhoneVerificationController HTTP 통합 테스트")
class PhoneVerificationControllerTest extends AbstractIntegrationTest {

    private static final String CODE_KEY_PREFIX = "phone:verify:";

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
