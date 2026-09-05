package com.recaring.care.fixture;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.care.vo.ReceivedRequestInfo;
import com.recaring.care.vo.WardInfo;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.SignUpType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

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

    public static CaregiverInfo createCaregiverInfo(String memberKey, CareRole careRole) {
        return new CaregiverInfo(memberKey, "보호자", GUARDIAN_PHONE, careRole);
    }

    public static WardInfo createWardInfo(String memberKey, CareRole careRole) {
        return createWardInfo(memberKey, null, careRole);
    }

    public static WardInfo createWardInfo(String memberKey, String wardNickname, CareRole careRole) {
        return new WardInfo(memberKey, "보호대상자", wardNickname, WARD_PHONE, Gender.FEMALE, careRole);
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

    // 승계 순서(먼저 등록된 쪽이 이긴다)를 검증하려면 등록 순서를 나타내는 id가 필요하다.
    public static CareRelationship createRelationship(String wardKey, String caregiverKey, CareRole careRole, long id) {
        CareRelationship relationship = CareRelationship.of(wardKey, caregiverKey, careRole);
        ReflectionTestUtils.setField(relationship, "id", id);
        return relationship;
    }

    public static CareRelationship createPrimaryGuardianRelationship(String wardKey, String caregiverKey) {
        return CareRelationship.of(wardKey, caregiverKey, CareRole.PRIMARY_GUARDIAN);
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
                .careRole(CareRole.PRIMARY_GUARDIAN)
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

    public static CareInvitation createManagerInvitation(String requesterKey, String targetKey, String wardKey) {
        return CareInvitation.builder()
                .requesterMemberKey(requesterKey)
                .targetMemberKey(targetKey)
                .wardMemberKey(wardKey)
                .careRole(CareRole.MANAGER)
                .build();
    }
}
