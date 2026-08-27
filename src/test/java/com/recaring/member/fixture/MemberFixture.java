package com.recaring.member.fixture;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.entity.SubscriptionType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public class MemberFixture {

    public static final String MEMBER_KEY = "test-member-key-uuid";
    public static final String OTHER_MEMBER_KEY = "other-member-key-uuid";

    public static final String PHONE = "01012345678";
    public static final String OTHER_PHONE = "01087654321";
    public static final String UNREGISTERED_PHONE = "01000000000";
    public static final String NAME = "홍길동";
    public static final LocalDate BIRTH = LocalDate.of(1990, 1, 1);
    public static final Gender GENDER = Gender.MALE;
    public static final MemberRole ROLE = MemberRole.GUARDIAN;

    public static final String UPDATED_NAME = "김철수";
    public static final LocalDate UPDATED_BIRTH = LocalDate.of(1995, 5, 5);

    public static final String EMAIL = "member@example.com";
    public static final String CURRENT_PASSWORD = "current1a";
    public static final String NEW_PASSWORD = "newpass2b";
    public static final String WRONG_PASSWORD = "wrongpw9z";

    public static Member createMember() {
        return createMember(PHONE, NAME, BIRTH, GENDER);
    }

    public static Member createMember(String phone) {
        return createMember(phone, NAME, BIRTH, GENDER);
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

    public static Member createWardMember(String phone) {
        return Member.builder()
                .phone(phone)
                .name("보호대상자")
                .birth(LocalDate.of(2000, 2, 2))
                .gender(Gender.FEMALE)
                .role(MemberRole.WARD)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    // The entity constructor always assigns BASIC; the persisted column can hold PREMIUM,
    // so tests reach that state through reflection instead of a production setter.
    public static Member createMemberWithSubscription(SubscriptionType subscriptionType) {
        Member member = createMember();
        ReflectionTestUtils.setField(member, "subscriptionType", subscriptionType);
        return member;
    }

    public static Member createMemberWithKey(String memberKey, String phone) {
        Member member = createMember(phone);
        ReflectionTestUtils.setField(member, "memberKey", memberKey);
        return member;
    }

    public static MembersTermsAgreement createTermsAgreement() {
        return createTermsAgreement(MEMBER_KEY);
    }

    public static MembersTermsAgreement createTermsAgreement(String memberKey) {
        return MembersTermsAgreement.builder()
                .memberKey(memberKey)
                .build();
    }
}
