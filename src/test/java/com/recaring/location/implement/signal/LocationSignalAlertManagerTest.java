package com.recaring.location.implement.signal;

import com.recaring.location.fixture.LocationFixture;
import com.recaring.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("위치 수집 중단 알림 선점 통합 테스트")
class LocationSignalAlertManagerTest extends AbstractIntegrationTest {

    @Autowired
    private LocationSignalAlertManager locationSignalAlertManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("같은 끊김 구간은 한 번만 선점된다")
    void claim_succeeds_only_once_per_outage() {
        boolean first = locationSignalAlertManager.claim(LocationFixture.WARD_KEY, LocationFixture.RECORDED_AT);
        boolean second = locationSignalAlertManager.claim(LocationFixture.WARD_KEY, LocationFixture.RECORDED_AT);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    @DisplayName("위치가 다시 들어온 뒤 또 끊기면 새 끊김 구간으로 다시 선점할 수 있다")
    void claim_succeeds_again_for_new_outage() {
        LocalDateTime reconnectedAt = LocationFixture.RECORDED_AT.plusHours(1);
        locationSignalAlertManager.claim(LocationFixture.WARD_KEY, LocationFixture.RECORDED_AT);

        boolean claimed = locationSignalAlertManager.claim(LocationFixture.WARD_KEY, reconnectedAt);

        assertThat(claimed).isTrue();
    }

    @Test
    @DisplayName("선점을 해제하면 같은 끊김 구간을 다시 선점할 수 있다")
    void claim_succeeds_again_after_release() {
        locationSignalAlertManager.claim(LocationFixture.WARD_KEY, LocationFixture.RECORDED_AT);
        locationSignalAlertManager.release(LocationFixture.WARD_KEY, LocationFixture.RECORDED_AT);

        boolean reclaimed = locationSignalAlertManager.claim(LocationFixture.WARD_KEY, LocationFixture.RECORDED_AT);

        assertThat(reclaimed).isTrue();
    }
}
