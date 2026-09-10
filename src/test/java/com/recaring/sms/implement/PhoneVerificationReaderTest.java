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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PhoneVerificationReader 통합 테스트")
class PhoneVerificationReaderTest extends AbstractIntegrationTest {

    private static final String CODE_KEY_PREFIX = "phone:verify:";

    @Autowired
    private PhoneVerificationReader phoneVerificationReader;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("저장된 인증 코드를 조회할 수 있다")
    void findCode_success() {
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + SmsFixture.PHONE, SmsFixture.CODE, 5, TimeUnit.MINUTES);
        PhoneNumber phone = SmsFixture.createPhoneNumber();

        SmsCode result = phoneVerificationReader.findCode(phone);

        assertThat(result.value()).isEqualTo(SmsFixture.CODE);
    }

    @Test
    @DisplayName("만료되거나 없는 인증 코드 조회 시 EXPIRED_VERIFICATION_CODE 예외가 발생한다")
    void findCode_fail_when_code_not_found() {
        PhoneNumber phone = SmsFixture.createPhoneNumber("01099999999");

        assertThatThrownBy(() -> phoneVerificationReader.findCode(phone))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorType())
                .isEqualTo(ErrorType.EXPIRED_VERIFICATION_CODE);
    }
}
