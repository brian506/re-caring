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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SiteInquiryRateLimitValidator 통합 테스트")
class SiteInquiryRateLimitValidatorTest extends AbstractIntegrationTest {

    private static final String SUBMIT_KEY = "site-inquiry:submit:" + SiteInquiryFixture.CLIENT_IP;
    private static final String VIEW_FAILURE_KEY = "site-inquiry:view-failure:" + SiteInquiryFixture.INQUIRY_KEY;
    private static final String DAILY_VIEW_FAILURE_KEY = "site-inquiry:view-failure-daily:" + SiteInquiryFixture.INQUIRY_KEY;
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
    @DisplayName("비밀번호 시도가 4회 쌓인 상태에서 5번째 시도는 허용한다")
    void fifth_view_attempt_is_allowed() {
        redisTemplate.opsForValue().set(VIEW_FAILURE_KEY, "4", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatCode(() -> rateLimitValidator.reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호 실패가 5회 쌓이면 6번째 시도부터 잠긴다")
    void sixth_view_attempt_after_five_failures_is_locked() {
        redisTemplate.opsForValue().set(VIEW_FAILURE_KEY, "5", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatThrownBy(() -> rateLimitValidator.reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_VIEW_LOCKED);
    }

    @Test
    @DisplayName("24시간 누적 실패가 19회인 상태에서 20번째 시도는 허용한다")
    void twentieth_daily_attempt_is_allowed() {
        redisTemplate.opsForValue().set(DAILY_VIEW_FAILURE_KEY, "19", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatCode(() -> rateLimitValidator.reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("10분 창이 비어 있어도 24시간 누적 실패가 20회면 잠긴다")
    void attempt_after_twenty_daily_failures_is_locked() {
        redisTemplate.opsForValue().set(DAILY_VIEW_FAILURE_KEY, "20", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        assertThatThrownBy(() -> rateLimitValidator.reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.SITE_INQUIRY_VIEW_LOCKED);
    }

    @Test
    @DisplayName("성공한 시도를 되돌리면 두 카운터가 1씩 줄어든다")
    void release_decrements_both_counters() {
        redisTemplate.opsForValue().set(VIEW_FAILURE_KEY, "3", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(DAILY_VIEW_FAILURE_KEY, "7", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        rateLimitValidator.releaseViewAttempt(SiteInquiryFixture.INQUIRY_KEY);

        assertThat(redisTemplate.opsForValue().get(VIEW_FAILURE_KEY)).isEqualTo("2");
        assertThat(redisTemplate.opsForValue().get(DAILY_VIEW_FAILURE_KEY)).isEqualTo("6");
    }

    @Test
    @DisplayName("되돌린 결과가 0이 되거나 키가 이미 만료됐으면 카운터 키를 남기지 않는다")
    void release_to_zero_or_after_expiry_leaves_no_key() {
        redisTemplate.opsForValue().set(VIEW_FAILURE_KEY, "1", SEEDED_TTL_SECONDS, TimeUnit.SECONDS);

        rateLimitValidator.releaseViewAttempt(SiteInquiryFixture.INQUIRY_KEY);

        assertThat(redisTemplate.hasKey(VIEW_FAILURE_KEY)).isFalse();
        assertThat(redisTemplate.hasKey(DAILY_VIEW_FAILURE_KEY)).isFalse();
    }

    @Test
    @DisplayName("동시에 몰린 비밀번호 시도도 창마다 5회까지만 통과한다")
    void concurrent_view_attempts_pass_only_up_to_quota() throws Exception {
        int requests = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger passed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                try {
                    rateLimitValidator.reserveViewAttempt(SiteInquiryFixture.INQUIRY_KEY);
                    passed.incrementAndGet();
                } catch (AppException ignored) {
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(passed.get()).isEqualTo(5);
    }
}
