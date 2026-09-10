package com.recaring.sms.implement;

import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.sms.vo.SmsCode;
import com.recaring.support.AbstractIntegrationTest;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("검증 토큰으로 전화번호를 꺼내면 토큰은 그 자리에서 사라져 두 번 쓸 수 없다")
    void consumePhoneByToken_returns_phone_and_invalidates_token() {
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        phoneVerificationWriter.add(phone, SmsFixture.createSmsCode());
        String token = phoneVerificationWriter.verify(phone);

        PhoneNumber consumed = phoneVerificationWriter.consumePhoneByToken(token);

        assertThat(consumed.value()).isEqualTo(SmsFixture.PHONE);
        assertThat(redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token)).isNull();
        assertThatThrownBy(() -> phoneVerificationWriter.consumePhoneByToken(token))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.NOT_VERIFIED_PHONE);
    }

    @Test
    @DisplayName("만료되거나 없는 토큰을 쓰면 NOT_VERIFIED_PHONE 예외가 발생한다")
    void consumePhoneByToken_fail_when_token_not_found() {
        String nonExistingToken = UUID.randomUUID().toString();

        assertThatThrownBy(() -> phoneVerificationWriter.consumePhoneByToken(nonExistingToken))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.NOT_VERIFIED_PHONE);
    }
}
