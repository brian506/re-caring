package com.recaring.location.fixture;

import com.recaring.location.vo.Gps;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LocationFixture {

    public static final String WARD_KEY = "ward-member-key-001";
    public static final String GUARDIAN_KEY = "guardian-member-key-001";
    public static final String MANAGER_KEY = "manager-member-key-001";
    public static final double LATITUDE = 37.5665;
    public static final double LONGITUDE = 126.9780;
    public static final Double ACCURACY = 15.0;
    public static final Integer BATTERY = 80;
    public static final int NOTIFIED_THRESHOLD = 20;
    public static final Double SPEED = 1.4;
    public static final String BATTERY_ALERT_STATE_KEY = "device:battery:" + WARD_KEY;
    public static final String LAST_NOTIFIED_THRESHOLD_FIELD = "lastNotifiedThreshold";
    public static final LocalDateTime MEASURED_AT = LocalDateTime.of(2026, 7, 27, 10, 15, 0);
    public static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 7, 27, 10, 15, 3);

    public static Gps createGps() {
        return createGps(BATTERY);
    }

    public static Gps createGps(Integer battery) {
        return new Gps(LATITUDE, LONGITUDE, RECORDED_AT, ACCURACY, battery, SPEED, MEASURED_AT);
    }

    public static Gps createGpsWithAccuracy(Double accuracy) {
        return new Gps(LATITUDE, LONGITUDE, RECORDED_AT, accuracy, BATTERY, SPEED, MEASURED_AT);
    }

    public static Member createWard() {
        return Member.builder()
                .phone("01011112222")
                .name("보호대상자")
                .birth(LocalDate.of(1950, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.WARD)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createGuardian() {
        return Member.builder()
                .phone("01033334444")
                .name("보호자")
                .birth(LocalDate.of(1980, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createManager() {
        return Member.builder()
                .phone("01055556666")
                .name("관리자")
                .birth(LocalDate.of(1975, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }
}
