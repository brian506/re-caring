package com.recaring.siteinquiry.implement;

import com.recaring.siteinquiry.fixture.SiteInquiryFixture;
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

@DisplayName("SiteInquiryRateLimitValidator 통합 테스트")
class SiteInquiryRateLimitValidatorTest extends AbstractIntegrationTest {

    private static final String SUBMIT_KEY = "site-inquiry:submit:" + SiteInquiryFixture.CLIENT_IP;
    private static final String VIEW_FAILURE_KEY = "site-inquiry:view-failure:" + SiteInquiryFixture.INQUIRY_KEY;
    private static final long WINDOW_SECONDS = 600;
    private static final long SEEDED_TTL_SECONDS = 300;

    @Autowired
    private SiteInquiryRateLimitValidator rateLimitValidator;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("10분 창에서 5번째 등록은 허용한다")
    void fifth_submit_in_window_is_allowed() {
        redisTemplate.opsForValue().set(SUBMIT_KEY, "4", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatCode(() -> rateLimitValidator.validateSubmit(SiteInquiryFixture.CLIENT_IP))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("6번째 등록은 한도 초과로 거부되고 창이 끝날 때까지 남은 초를 담는다")
    void sixth_submit_in_window_is_rejected_with_remaining_seconds() {
        redisTemplate.opsForValue().set(SUBMIT_KEY, "5", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatThrownBy(() -> rateLimitValidator.validateSubmit(SiteInquiryFixture.CLIENT_IP))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_SUBMIT_RATE_LIMITED)
                .satisfies(e -> assertThat(((RetryAfter) ((AppException) e).getData()).seconds())
                        .isBetween(SEEDED_TTL_SECONDS - 10, SEEDED_TTL_SECONDS));
    }

    @Test
    @DisplayName("창의 첫 등록이면 카운터에 10분 만료가 걸린다")
    void first_submit_in_window_sets_ten_minute_expiry() {
        rateLimitValidator.validateSubmit(SiteInquiryFixture.CLIENT_IP);

        Long ttl = redisTemplate.getExpire(SUBMIT_KEY, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(WINDOW_SECONDS - 10, WINDOW_SECONDS);
    }

    @Test
    @DisplayName("비밀번호 실패가 4회일 때까지는 조회를 막지 않는다")
    void four_view_failures_do_not_lock() {
        redisTemplate.opsForValue().set(VIEW_FAILURE_KEY, "4", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatCode(() -> rateLimitValidator.validateViewNotLocked(SiteInquiryFixture.INQUIRY_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호 실패가 5회가 되면 조회가 잠긴다")
    void five_view_failures_lock_the_inquiry() {
        redisTemplate.opsForValue().set(VIEW_FAILURE_KEY, "5", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatThrownBy(() -> rateLimitValidator.validateViewNotLocked(SiteInquiryFixture.INQUIRY_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_VIEW_LOCKED);
    }
}
