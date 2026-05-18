package com.recaring.safezone.fixture;

import com.recaring.care.fixture.CareFixture;
import com.recaring.safezone.controller.request.CreateSafeZoneCommand;
import com.recaring.safezone.controller.request.UpdateSafeZoneCommand;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.entity.SafeZoneRadius;
import com.recaring.safezone.vo.SafeZoneInfo;

public class SafeZoneFixture {

    public static final String WARD_MEMBER_KEY = CareFixture.WARD_MEMBER_KEY;
    public static final String NAME = "우리집";
    public static final String ADDRESS = "서울시 강남구 테헤란로 1";
    public static final double LATITUDE = 37.5000;
    public static final double LONGITUDE = 127.0000;
    public static final SafeZoneRadius RADIUS = SafeZoneRadius.MEDIUM;

    public static final String UPDATED_NAME = "직장";
    public static final String UPDATED_ADDRESS = "서울시 서초구 반포대로 2";
    public static final double UPDATED_LATITUDE = 37.4900;
    public static final double UPDATED_LONGITUDE = 127.0100;
    public static final SafeZoneRadius UPDATED_RADIUS = SafeZoneRadius.LARGE;

    public static SafeZone createSafeZone() {
        return SafeZone.builder()
                .wardMemberKey(WARD_MEMBER_KEY)
                .name(NAME)
                .address(ADDRESS)
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .radius(RADIUS)
                .build();
    }

    public static SafeZone createSafeZone(String wardMemberKey) {
        return SafeZone.builder()
                .wardMemberKey(wardMemberKey)
                .name(NAME)
                .address(ADDRESS)
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .radius(RADIUS)
                .build();
    }

    public static SafeZoneInfo createSafeZoneInfo(String safeZoneKey) {
        return new SafeZoneInfo(safeZoneKey, NAME, ADDRESS, LATITUDE, LONGITUDE, RADIUS);
    }

    public static CreateSafeZoneCommand createCommand() {
        return new CreateSafeZoneCommand(WARD_MEMBER_KEY, NAME, ADDRESS, LATITUDE, LONGITUDE, RADIUS);
    }

    public static UpdateSafeZoneCommand updateCommand() {
        return new UpdateSafeZoneCommand(UPDATED_NAME, UPDATED_ADDRESS, UPDATED_LATITUDE, UPDATED_LONGITUDE, UPDATED_RADIUS);
    }
}
