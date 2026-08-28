package com.recaring.safezone.vo;

import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import com.recaring.safezone.fixture.SafeZoneFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SafeZoneInfo 단위 테스트")
class SafeZoneInfoTest {

    private static final String KEY = SafeZoneFixture.SAFE_ZONE_KEY;

    // 위도 1도는 약 111,195m. 아래 오프셋은 모두 이 값에서 환산한 것이다.
    private static final double LAT_OFFSET_ABOUT_556_METERS = 0.005;
    private static final double LAT_OFFSET_ABOUT_990_METERS = 0.0089;
    private static final double LAT_OFFSET_ABOUT_1012_METERS = 0.0091;
    private static final double LAT_OFFSET_ABOUT_2224_METERS = 0.02;

    // 위도 37.5도에서 경도 1도는 약 88,300m. 0.02도는 약 1,766m다.
    private static final double LNG_OFFSET_ABOUT_1766_METERS = 0.02;

    @Test
    @DisplayName("중심 좌표는 존 안으로 판정한다")
    void contains_center() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(KEY);

        boolean result = zone.contains(SafeZoneFixture.LATITUDE, SafeZoneFixture.LONGITUDE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("반경 안의 좌표는 존 안으로 판정한다")
    void contains_inside_radius() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(KEY);

        boolean result = zone.contains(
                SafeZoneFixture.LATITUDE + LAT_OFFSET_ABOUT_556_METERS, SafeZoneFixture.LONGITUDE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("반경 밖의 좌표는 존 밖으로 판정한다")
    void contains_outside_radius() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(KEY);

        boolean result = zone.contains(
                SafeZoneFixture.LATITUDE + LAT_OFFSET_ABOUT_2224_METERS, SafeZoneFixture.LONGITUDE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("반경 경계 직전(약 990m)은 존 안, 직후(약 1,012m)는 존 밖으로 판정한다")
    void contains_flips_at_radius_boundary() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfoWithRadius(KEY, SafeZoneRadius.MEDIUM);

        assertThat(zone.contains(
                SafeZoneFixture.LATITUDE + LAT_OFFSET_ABOUT_990_METERS, SafeZoneFixture.LONGITUDE)).isTrue();
        assertThat(zone.contains(
                SafeZoneFixture.LATITUDE + LAT_OFFSET_ABOUT_1012_METERS, SafeZoneFixture.LONGITUDE)).isFalse();
    }

    @Test
    @DisplayName("남쪽으로 벗어난 좌표도 북쪽과 동일하게 존 밖으로 판정한다")
    void contains_outside_radius_to_the_south() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(KEY);

        boolean result = zone.contains(
                SafeZoneFixture.LATITUDE - LAT_OFFSET_ABOUT_2224_METERS, SafeZoneFixture.LONGITUDE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("경도 방향으로 반경을 벗어나도 존 밖으로 판정한다")
    void contains_outside_radius_by_longitude() {
        SafeZoneInfo zone = SafeZoneFixture.createSafeZoneInfo(KEY);

        boolean result = zone.contains(
                SafeZoneFixture.LATITUDE, SafeZoneFixture.LONGITUDE + LNG_OFFSET_ABOUT_1766_METERS);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("같은 좌표라도 반경이 작으면 존 밖으로 판정한다")
    void contains_depends_on_radius() {
        SafeZoneInfo small = SafeZoneFixture.createSafeZoneInfoWithRadius(KEY, SafeZoneRadius.SMALL);
        SafeZoneInfo medium = SafeZoneFixture.createSafeZoneInfoWithRadius(KEY, SafeZoneRadius.MEDIUM);
        double latitude = SafeZoneFixture.LATITUDE + LAT_OFFSET_ABOUT_556_METERS;

        assertThat(small.contains(latitude, SafeZoneFixture.LONGITUDE)).isFalse();
        assertThat(medium.contains(latitude, SafeZoneFixture.LONGITUDE)).isTrue();
    }
}
