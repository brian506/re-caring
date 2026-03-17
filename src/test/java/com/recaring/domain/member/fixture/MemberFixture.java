package com.recaring.domain.member.fixture;

import com.recaring.domain.member.Gender;
import com.recaring.domain.member.Member;
import com.recaring.domain.member.MemberRole;

import java.time.LocalDate;

public class MemberFixture {

    public static final String EMAIL = "test@example.com";
    public static final String PHONE = "01012345678";
    public static final String ENCODED_PASSWORD = "$2a$10$encoded";
    public static final String NAME = "홍길동";
    public static final LocalDate BIRTH = LocalDate.of(1990, 1, 1);
    public static final Gender GENDER = Gender.MALE;
    public static final MemberRole ROLE = MemberRole.GUARDIAN;


    public static Member createMember() {
        return Member.create(EMAIL, PHONE, ENCODED_PASSWORD, NAME, BIRTH, GENDER, ROLE);
    }


    public static Member createMember(String email) {
        return Member.create(email, PHONE, ENCODED_PASSWORD, NAME, BIRTH, GENDER, ROLE);
    }


    public static Member createMember(String email, String phone) {
        return Member.create(email, phone, ENCODED_PASSWORD, NAME, BIRTH, GENDER, ROLE);
    }


    public static Member createMember(String email, String phone, String name, LocalDate birth, Gender gender) {
        return Member.create(email, phone, ENCODED_PASSWORD, name, birth, gender, ROLE);
    }
}
