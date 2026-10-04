package com.recaring.safezone.fixture;

import com.recaring.care.fixture.CareFixture;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import com.recaring.safezone.dataaccess.entity.SafeZoneType;
import com.recaring.safezone.vo.SafeZoneCreation;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.safezone.vo.SafeZoneUpdate;

public class SafeZoneFixture {

    public static final String WARD_MEMBER_KEY = CareFixture.WARD_MEMBER_KEY;
    public static final String SAFE_ZONE_KEY = "safe-zone-key-001";
    public static final String UNKNOWN_SAFE_ZONE_KEY = "unknown-safe-zone-key";

    public static final String NAME = "우리집";
    public static final String ADDRESS = "서울시 강남구 테헤란로 1";
    public static final double LATITUDE = 37.5000;
    public static final double LONGITUDE = 127.0000;
    public static final SafeZoneRadius RADIUS = SafeZoneRadius.MEDIUM;
    public static final SafeZoneType TYPE = SafeZoneType.SAFE;

    public static final String DANGER_ZONE_NAME = "공사장";

    public static final String UPDATED_NAME = "직장";
    public static final String UPDATED_ADDRESS = "서울시 서초구 반포대로 2";
    public static final double UPDATED_LATITUDE = 37.4900;
    public static final double UPDATED_LONGITUDE = 127.0100;
    public static final SafeZoneRadius UPDATED_RADIUS = SafeZoneRadius.LARGE;

    public static SafeZone createSafeZone() {
        return createSafeZone(WARD_MEMBER_KEY);
    }

    public static SafeZone createSafeZone(String wardMemberKey) {
        return SafeZone.builder()
                .wardMemberKey(wardMemberKey)
                .name(NAME)
                .address(ADDRESS)
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .radius(RADIUS)
                .type(TYPE)
                .build();
    }

    public static SafeZone createDangerZone(String wardMemberKey) {
        return SafeZone.builder()
                .wardMemberKey(wardMemberKey)
                .name(DANGER_ZONE_NAME)
                .address(ADDRESS)
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .radius(RADIUS)
                .type(SafeZoneType.DANGER)
                .build();
    }

    public static SafeZoneInfo createSafeZoneInfo(String safeZoneKey) {
        return new SafeZoneInfo(safeZoneKey, NAME, ADDRESS, LATITUDE, LONGITUDE, RADIUS, TYPE);
    }

    public static SafeZoneInfo createSafeZoneInfoAt(
            String safeZoneKey, String name, double latitude, double longitude) {
        return new SafeZoneInfo(safeZoneKey, name, ADDRESS, latitude, longitude, RADIUS, TYPE);
    }

    public static SafeZoneInfo createDangerZoneInfoAt(
            String safeZoneKey, String name, double latitude, double longitude) {
        return new SafeZoneInfo(safeZoneKey, name, ADDRESS, latitude, longitude, RADIUS, SafeZoneType.DANGER);
    }

    public static SafeZoneInfo createSafeZoneInfoWithRadius(String safeZoneKey, SafeZoneRadius radius) {
        return new SafeZoneInfo(safeZoneKey, NAME, ADDRESS, LATITUDE, LONGITUDE, radius, TYPE);
    }

    public static SafeZoneCreation createCommand() {
        return new SafeZoneCreation(WARD_MEMBER_KEY, NAME, ADDRESS, LATITUDE, LONGITUDE, RADIUS, TYPE);
    }

    public static SafeZoneUpdate updateCommand() {
        return new SafeZoneUpdate(UPDATED_NAME, UPDATED_ADDRESS, UPDATED_LATITUDE, UPDATED_LONGITUDE, UPDATED_RADIUS, TYPE);
    }
}
