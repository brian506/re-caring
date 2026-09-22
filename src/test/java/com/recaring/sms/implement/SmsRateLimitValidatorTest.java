package com.recaring.sms.implement;

import com.recaring.sms.fixture.SmsFixture;
import com.recaring.support.AbstractIntegrationTest;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import com.recaring.support.response.RetryAfter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SmsRateLimitValidator 통합 테스트")
class SmsRateLimitValidatorTest extends AbstractIntegrationTest {

    private static final String QUOTA_KEY = "sms:quota:" + SmsFixture.PHONE;
    private static final long WINDOW_SECONDS = 3600;
    private static final long SEEDED_TTL_SECONDS = 1800;

    @Autowired
    private SmsRateLimitValidator smsRateLimitValidator;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("한 시간 창에서 5번째 요청은 통과한다")
    void fifth_request_in_window_is_allowed() {
        redisTemplate.opsForValue().set(QUOTA_KEY, "4", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatCode(() -> smsRateLimitValidator.validate(SmsFixture.createPhoneNumber()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("6번째 요청은 한도 초과로 거부되고 창이 끝날 때까지 남은 초를 담는다")
    void sixth_request_in_window_is_rejected_with_remaining_seconds() {
        redisTemplate.opsForValue().set(QUOTA_KEY, "5", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatThrownBy(() -> smsRateLimitValidator.validate(SmsFixture.createPhoneNumber()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SMS_SEND_QUOTA_EXCEEDED)
                .satisfies(e -> assertThat(((RetryAfter) ((AppException) e).getData()).seconds())
                        .isBetween(SEEDED_TTL_SECONDS - 10, SEEDED_TTL_SECONDS));
    }

    @Test
    @DisplayName("창의 첫 요청이면 카운터에 1시간 만료가 걸린다")
    void first_request_in_window_sets_one_hour_expiry() {
        smsRateLimitValidator.validate(SmsFixture.createPhoneNumber());

        Long ttl = redisTemplate.getExpire(QUOTA_KEY, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(WINDOW_SECONDS - 10, WINDOW_SECONDS);
    }
}
