package com.recaring.location.fixture;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;

import java.time.LocalDate;

public class LocationFixture {

    public static final String WARD_KEY = "ward-member-key-001";
    public static final String GUARDIAN_KEY = "guardian-member-key-001";
    public static final double LATITUDE = 37.5665;
    public static final double LONGITUDE = 126.9780;

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
}
