package com.recaring.safezone.fixture;

import com.recaring.safezone.event.SafeZoneExitedEvent;
import com.recaring.safezone.event.SafeZoneEnteredEvent;
import com.recaring.safezone.dataaccess.entity.SafeZoneState;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.care.fixture.CareFixture;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.vo.SafeZoneType;
import com.recaring.safezone.vo.SafeZoneCreation;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.safezone.vo.SafeZoneRadius;
import com.recaring.safezone.vo.SafeZoneUpdate;

public class SafeZoneFixture {

    public static final String WARD_MEMBER_KEY = CareFixture.WARD_MEMBER_KEY;
    public static final String SAFE_ZONE_KEY = "safe-zone-key-001";
    public static final String UNKNOWN_SAFE_ZONE_KEY = "unknown-safe-zone-key";

    public static final String NAME = "우리집";
    public static final String ADDRESS = "서울시 강남구 테헤란로 1";
    public static final double LATITUDE = 37.5000;
    public static final double LONGITUDE = 127.0000;
    public static final int RADIUS_METERS = 1000;
    public static final SafeZoneType TYPE = SafeZoneType.SAFE;

    public static final String DANGER_ZONE_NAME = "공사장";

    public static final String UPDATED_NAME = "직장";
    public static final String UPDATED_ADDRESS = "서울시 서초구 반포대로 2";
    public static final double UPDATED_LATITUDE = 37.4900;
    public static final double UPDATED_LONGITUDE = 127.0100;
    public static final int UPDATED_RADIUS_METERS = 1500;

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
                .radiusMeters(RADIUS_METERS)
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
                .radiusMeters(RADIUS_METERS)
                .type(SafeZoneType.DANGER)
                .build();
    }

    public static SafeZoneInfo createSafeZoneInfo(String safeZoneKey) {
        return new SafeZoneInfo(safeZoneKey, NAME, ADDRESS, LATITUDE, LONGITUDE, RADIUS_METERS, TYPE);
    }

    public static SafeZoneInfo createSafeZoneInfoAt(
            String safeZoneKey, String name, double latitude, double longitude) {
        return new SafeZoneInfo(safeZoneKey, name, ADDRESS, latitude, longitude, RADIUS_METERS, TYPE);
    }

    public static SafeZoneInfo createDangerZoneInfoAt(
            String safeZoneKey, String name, double latitude, double longitude) {
        return new SafeZoneInfo(safeZoneKey, name, ADDRESS, latitude, longitude, RADIUS_METERS, SafeZoneType.DANGER);
    }

    public static SafeZoneInfo createSafeZoneInfoAt(
            String safeZoneKey, double latitude, double longitude, int radiusMeters) {
        return new SafeZoneInfo(safeZoneKey, NAME, ADDRESS, latitude, longitude, radiusMeters, TYPE);
    }

    public static SafeZoneInfo createSafeZoneInfoWithRadius(String safeZoneKey, int radiusMeters) {
        return new SafeZoneInfo(safeZoneKey, NAME, ADDRESS, LATITUDE, LONGITUDE, radiusMeters, TYPE);
    }

    public static SafeZoneCreation createCommand() {
        return new SafeZoneCreation(WARD_MEMBER_KEY, NAME, ADDRESS, LATITUDE, LONGITUDE, new SafeZoneRadius(RADIUS_METERS), TYPE);
    }

    public static SafeZoneUpdate updateCommand() {
        return new SafeZoneUpdate(UPDATED_NAME, UPDATED_ADDRESS, UPDATED_LATITUDE, UPDATED_LONGITUDE, new SafeZoneRadius(UPDATED_RADIUS_METERS), TYPE);
    }

    public static SafeZoneEnteredEvent createSafeZoneEnteredEvent() {
        return new SafeZoneEnteredEvent(
                LocationFixture.WARD_KEY, SAFE_ZONE_KEY, LocationFixture.SAFE_ZONE_NAME, SafeZoneType.SAFE, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocationFixture.DETECTED_AT);
    }

    public static SafeZoneEnteredEvent createDangerZoneEnteredEvent() {
        return new SafeZoneEnteredEvent(
                LocationFixture.WARD_KEY, LocationFixture.DANGER_ZONE_KEY, DANGER_ZONE_NAME, SafeZoneType.DANGER, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocationFixture.DETECTED_AT);
    }

    public static SafeZoneExitedEvent createSafeZoneExitedEvent() {
        return new SafeZoneExitedEvent(
                LocationFixture.WARD_KEY, SAFE_ZONE_KEY, LocationFixture.SAFE_ZONE_NAME, SafeZoneType.SAFE, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocationFixture.DETECTED_AT);
    }

    public static SafeZoneExitedEvent createDangerZoneExitedEvent() {
        return new SafeZoneExitedEvent(
                LocationFixture.WARD_KEY, LocationFixture.DANGER_ZONE_KEY, DANGER_ZONE_NAME, SafeZoneType.DANGER, LocationFixture.LATITUDE, LocationFixture.LONGITUDE, LocationFixture.DETECTED_AT);
    }

    public static SafeZoneState createSafeZoneState(String safeZoneKeysCsv) {
        return SafeZoneState.builder()
                .wardMemberKey(LocationFixture.WARD_KEY)
                .safeZoneKeys(safeZoneKeysCsv)
                .build();
    }
}
