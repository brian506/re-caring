package com.recaring.sms.implement;

import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PhoneVerificationWriter 통합 테스트")
class PhoneVerificationWriterTest extends AbstractIntegrationTest {

    private static final String CODE_KEY_PREFIX = "phone:verify:";
    private static final String TOKEN_KEY_PREFIX = "phone:token:";

    @Autowired
    private PhoneVerificationWriter phoneVerificationWriter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("add() 호출 시 인증 코드가 Redis에 저장된다")
    void add_stores_code_in_redis() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        SmsCode code = SmsFixture.createSmsCode();

        phoneVerificationWriter.add(phone, code);

        String stored = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + SmsFixture.PHONE);
        assertThat(stored).isEqualTo(SmsFixture.CODE);
    }

    @Test
    @DisplayName("verify() 호출 시 인증 코드가 삭제되고 토큰이 저장된다")
    void verify_deletes_code_and_stores_token() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        phoneVerificationWriter.add(phone, SmsFixture.createSmsCode());

        String token = phoneVerificationWriter.verify(phone);

        // 코드는 삭제됨
        String storedCode = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + SmsFixture.PHONE);
        assertThat(storedCode).isNull();

        // 토큰은 저장됨
        String storedPhone = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        assertThat(storedPhone).isEqualTo(SmsFixture.PHONE);
    }

    @Test
    @DisplayName("verify() 호출 시 UUID 형식의 토큰이 반환된다")
    void verify_returns_uuid_token() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        phoneVerificationWriter.add(phone, SmsFixture.createSmsCode());

        String token = phoneVerificationWriter.verify(phone);

        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("deleteToken() 호출 시 토큰이 Redis에서 삭제된다")
    void deleteToken_removes_token_from_redis() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        phoneVerificationWriter.add(phone, SmsFixture.createSmsCode());
        String token = phoneVerificationWriter.verify(phone);

        phoneVerificationWriter.deleteToken(token);

        String stored = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        assertThat(stored).isNull();
    }
}
