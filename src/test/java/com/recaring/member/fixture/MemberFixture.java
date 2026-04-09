package com.recaring.member.fixture;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;

import java.time.LocalDate;

public class MemberFixture {

    public static final String PHONE = "01012345678";
    public static final String NAME = "홍길동";
    public static final LocalDate BIRTH = LocalDate.of(1990, 1, 1);
    public static final Gender GENDER = Gender.MALE;
    public static final MemberRole ROLE = MemberRole.GUARDIAN;

    public static Member createMember() {
        return Member.builder()
                .phone(PHONE)
                .name(NAME)
                .birth(BIRTH)
                .gender(GENDER)
                .role(ROLE)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createMember(String phone) {
        return Member.builder()
                .phone(phone)
                .name(NAME)
                .birth(BIRTH)
                .gender(GENDER)
                .role(ROLE)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createMember(String phone, String name, LocalDate birth, Gender gender) {
        return Member.builder()
                .phone(phone)
                .name(name)
                .birth(birth)
                .gender(gender)
                .role(ROLE)
                .signUpType(SignUpType.LOCAL)
                .build();
    }
}
