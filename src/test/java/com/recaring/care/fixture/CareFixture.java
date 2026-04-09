package com.recaring.care.fixture;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;

public class CareFixture {

    public static final String GUARDIAN_MEMBER_KEY = "guardian-member-key";
    public static final String WARD_MEMBER_KEY = "ward-member-key";
    public static final String MANAGER_MEMBER_KEY = "manager-member-key";
    public static final String REQUEST_KEY = "test-request-key";

    public static final String GUARDIAN_PHONE = "01011112222";
    public static final String WARD_PHONE = "01033334444";
    public static final String MANAGER_PHONE = "01055556666";

    public static Member createGuardianMember() {
        return Member.builder()
                .phone(GUARDIAN_PHONE)
                .name("보호자")
                .birth(java.time.LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createGuardianMember(String phone) {
        return Member.builder()
                .phone(phone)
                .name("보호자")
                .birth(java.time.LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .role(MemberRole.GUARDIAN)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createWardMember() {
        return Member.builder()
                .phone(WARD_PHONE)
                .name("보호대상자")
                .birth(java.time.LocalDate.of(2000, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.WARD)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static Member createWardMember(String phone) {
        return Member.builder()
                .phone(phone)
                .name("보호대상자")
                .birth(java.time.LocalDate.of(2000, 1, 1))
                .gender(Gender.FEMALE)
                .role(MemberRole.WARD)
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public static CareRelationship createGuardianRelationship(String wardKey, String caregiverKey) {
        return CareRelationship.of(wardKey, caregiverKey, CareRole.GUARDIAN);
    }

    public static CareRelationship createManagerRelationship(String wardKey, String managerKey) {
        return CareRelationship.of(wardKey, managerKey, CareRole.MANAGER);
    }

    public static CareInvitation createWardInvitation(String requesterKey, String wardKey) {
        return CareInvitation.builder()
                .requesterMemberKey(requesterKey)
                .targetMemberKey(wardKey)
                .wardMemberKey(wardKey)
                .careRole(CareRole.GUARDIAN)
                .build();
    }

    public static CareInvitation createManagerInvitation(String requesterKey, String targetKey, String wardKey) {
        return CareInvitation.builder()
                .requesterMemberKey(requesterKey)
                .targetMemberKey(targetKey)
                .wardMemberKey(wardKey)
                .careRole(CareRole.MANAGER)
                .build();
    }
}
