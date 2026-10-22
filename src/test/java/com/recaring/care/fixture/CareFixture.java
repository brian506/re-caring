package com.recaring.care.fixture;

import com.recaring.member.vo.MemberInfo;
import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.vo.CareRole;
import com.recaring.care.dataaccess.entity.DesignatedAvatar;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.care.vo.ReceivedRequestInfo;
import com.recaring.care.vo.WardInfo;
import com.recaring.member.vo.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.vo.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;

import java.time.LocalDateTime;

public class CareFixture {

    public static final String GUARDIAN_MEMBER_KEY = "guardian-member-key";
    public static final String WARD_MEMBER_KEY = "ward-member-key";
    public static final String SECOND_GUARDIAN_MEMBER_KEY = "second-guardian-member-key";
    public static final String REQUEST_KEY = "test-request-key";
    // Sorts before every other member key; used to pin the lock order when the ward key is the smaller one.
    public static final String LOW_SORTING_WARD_MEMBER_KEY = "aaa-ward-member-key";

    public static final String GUARDIAN_PHONE = "01011112222";
    public static final String WARD_PHONE = "01033334444";
    public static final String SECOND_GUARDIAN_PHONE = "01055556666";
    public static final String OTHER_GUARDIAN_PHONE = "01077778888";
    public static final String OTHER_WARD_PHONE = "01099990000";
    public static final LocalDateTime WARD_JOINED_AT = LocalDateTime.of(2026, 8, 14, 10, 23);
    public static final String WARD_JOINED_AT_TEXT = "2026-08-14T10:23:00";

    public static final String SENIOR_AVATAR_CODE = "senior_female_1";
    public static final String OTHER_SENIOR_AVATAR_CODE = "senior_female_2";
    public static final String ADULT_AVATAR_CODE = "adult_male_1";
    public static final String UNKNOWN_AVATAR_CODE = "senior_female_5";

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

    public static MemberInfo createWardMemberInfoWithAvatar(String profileAvatarCode) {
        Member ward = createWardMember();
        ward.changeProfileAvatarCode(profileAvatarCode);
        return MemberInfo.from(ward);
    }

    public static MemberInfo createGuardianMemberInfoWithAvatar(String profileAvatarCode) {
        Member guardian = createGuardianMember();
        guardian.changeProfileAvatarCode(profileAvatarCode);
        return MemberInfo.from(guardian);
    }

    public static CaregiverInfo createCaregiverInfo(String memberKey, CareRole careRole) {
        return createCaregiverInfo(memberKey, careRole, null, null);
    }

    public static CaregiverInfo createCaregiverInfo(String memberKey, CareRole careRole,
                                                    String profileAvatarCode, String designatedProfileAvatarCode) {
        return new CaregiverInfo(memberKey, "보호자", GUARDIAN_PHONE, careRole,
                profileAvatarCode, designatedProfileAvatarCode, null);
    }

    public static WardInfo createWardInfo(String memberKey, CareRole careRole) {
        return createWardInfo(memberKey, null, careRole);
    }

    public static WardInfo createWardInfo(String memberKey, String wardNickname, CareRole careRole) {
        return createWardInfo(memberKey, wardNickname, careRole, null, null);
    }

    public static WardInfo createWardInfo(String memberKey, String wardNickname, CareRole careRole,
                                          String wardProfileAvatarCode, String designatedProfileAvatarCode) {
        return new WardInfo(memberKey, "보호대상자", wardNickname, WARD_PHONE, Gender.FEMALE, careRole,
                wardProfileAvatarCode, designatedProfileAvatarCode, null);
    }

    public static DesignatedAvatar createDesignatedAvatar(String ownerKey, String wardKey, String targetKey, String code) {
        return DesignatedAvatar.builder()
                .ownerMemberKey(ownerKey)
                .wardMemberKey(wardKey)
                .targetMemberKey(targetKey)
                .profileAvatarCode(code)
                .build();
    }

    public static CareRelationshipRegistration createRegistration(String wardKey, String caregiverKey, CareRole careRole) {
        return new CareRelationshipRegistration(wardKey, caregiverKey, careRole);
    }

    public static ReceivedRequestInfo createReceivedRequestInfo(
            String requestKey, String requesterKey, String wardKey, CareRole careRole, LocalDateTime createdAt) {
        return new ReceivedRequestInfo(
                requestKey, requesterKey, "보호자", wardKey, "보호대상자",
                careRole, CareInvitationStatus.PENDING, createdAt);
    }

    public static CareRelationship createGuardianRelationship(String wardKey, String caregiverKey) {
        return CareRelationship.of(wardKey, caregiverKey, CareRole.GUARDIAN);
    }

    public static CareInvitation createWardInvitation(String requesterKey, String wardKey) {
        return CareInvitation.builder()
                .requesterMemberKey(requesterKey)
                .targetMemberKey(wardKey)
                .wardMemberKey(wardKey)
                .careRole(CareRole.GUARDIAN)
                .build();
    }

    public static CareInvitation createGuardianInvitation(String requesterKey, String targetKey, String wardKey) {
        return CareInvitation.builder()
                .requesterMemberKey(requesterKey)
                .targetMemberKey(targetKey)
                .wardMemberKey(wardKey)
                .careRole(CareRole.GUARDIAN)
                .build();
    }

    public static CareInvitation createAcceptedWardInvitation(String requesterKey, String wardKey) {
        CareInvitation invitation = createWardInvitation(requesterKey, wardKey);
        invitation.accept();
        return invitation;
    }

    public static CareInvitation createRejectedWardInvitation(String requesterKey, String wardKey) {
        CareInvitation invitation = createWardInvitation(requesterKey, wardKey);
        invitation.reject();
        return invitation;
    }
}
