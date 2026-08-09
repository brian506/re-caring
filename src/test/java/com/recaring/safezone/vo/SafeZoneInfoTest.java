package com.recaring.safezone.vo;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import com.recaring.safezone.fixture.SafeZoneFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SafeZoneInfo 단위 테스트")
class SafeZoneInfoTest {

    private static final String SAFE_ZONE_KEY = "safe-zone-key-001";

    // 위도 1도는 약 111,195m. 0.005도는 약 556m로 SMALL(500m) 밖이자 MEDIUM(1000m) 안이다.
    private static final double OFFSET_ABOUT_556_METERS = 0.005;
    private static final double OFFSET_ABOUT_2224_METERS = 0.02;

    @Test
    @DisplayName("중심 좌표는 존 안으로 판정한다")
    void contains_center() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(SAFE_ZONE_KEY);

        boolean result = zone.contains(SafeZoneFixture.LATITUDE, SafeZoneFixture.LONGITUDE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("반경 안의 좌표는 존 안으로 판정한다")
    void contains_inside_radius() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(SAFE_ZONE_KEY);

        boolean result = zone.contains(
                SafeZoneFixture.LATITUDE + OFFSET_ABOUT_556_METERS, SafeZoneFixture.LONGITUDE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("반경 밖의 좌표는 존 밖으로 판정한다")
    void contains_outside_radius() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(SAFE_ZONE_KEY);

        boolean result = zone.contains(
                SafeZoneFixture.LATITUDE + OFFSET_ABOUT_2224_METERS, SafeZoneFixture.LONGITUDE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("같은 좌표라도 반경이 작으면 존 밖으로 판정한다")
    void contains_depends_on_radius() {
        SafeZoneInfo small = SafeZoneFixture.createSafeZoneInfoWithRadius(SAFE_ZONE_KEY, SafeZoneRadius.SMALL);
        SafeZoneInfo medium = SafeZoneFixture.createSafeZoneInfoWithRadius(SAFE_ZONE_KEY, SafeZoneRadius.MEDIUM);
        double latitude = SafeZoneFixture.LATITUDE + OFFSET_ABOUT_556_METERS;

        assertThat(small.contains(latitude, SafeZoneFixture.LONGITUDE)).isFalse();
        assertThat(medium.contains(latitude, SafeZoneFixture.LONGITUDE)).isTrue();
    }

    @Test
    @DisplayName("경도 방향으로 반경을 벗어나도 존 밖으로 판정한다")
    void contains_outside_radius_by_longitude() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(SAFE_ZONE_KEY);

        boolean result = zone.contains(
                SafeZoneFixture.LATITUDE, SafeZoneFixture.LONGITUDE + OFFSET_ABOUT_2224_METERS);

        assertThat(result).isFalse();
    }
}
