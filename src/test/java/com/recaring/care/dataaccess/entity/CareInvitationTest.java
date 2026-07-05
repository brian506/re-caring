package com.recaring.care.dataaccess.entity;

import com.recaring.care.fixture.CareFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CareInvitation 엔티티 단위 테스트")
class CareInvitationTest {

    @Test
    @DisplayName("targetRole - 초대 대상이 보호 대상자(target == ward)이면 WARD를 반환한다")
    void targetRole_returns_ward_when_target_is_ward() {
        CareInvitation invitation = CareInvitation.builder()
                .requesterMemberKey(CareFixture.GUARDIAN_MEMBER_KEY)
                .targetMemberKey(CareFixture.WARD_MEMBER_KEY)
                .wardMemberKey(CareFixture.WARD_MEMBER_KEY)
                .careRole(CareRole.GUARDIAN)
                .build();

        assertThat(invitation.targetRole()).isEqualTo(CarePartyRole.WARD);
    }

    @Test
    @DisplayName("targetRole - careRole이 GUARDIAN인 관계자 외 초대는 GUARDIAN을 반환한다")
    void targetRole_returns_guardian_when_care_role_is_guardian() {
        CareInvitation invitation = CareInvitation.builder()
                .requesterMemberKey(CareFixture.GUARDIAN_MEMBER_KEY)
                .targetMemberKey(CareFixture.MANAGER_MEMBER_KEY)
                .wardMemberKey(CareFixture.WARD_MEMBER_KEY)
                .careRole(CareRole.GUARDIAN)
                .build();

        assertThat(invitation.targetRole()).isEqualTo(CarePartyRole.GUARDIAN);
    }

    @Test
    @DisplayName("targetRole - careRole이 MANAGER이면 MANAGER를 반환한다")
    void targetRole_returns_manager_when_care_role_is_manager() {
        CareInvitation invitation = CareInvitation.builder()
                .requesterMemberKey(CareFixture.GUARDIAN_MEMBER_KEY)
                .targetMemberKey(CareFixture.MANAGER_MEMBER_KEY)
                .wardMemberKey(CareFixture.WARD_MEMBER_KEY)
                .careRole(CareRole.MANAGER)
                .build();

        assertThat(invitation.targetRole()).isEqualTo(CarePartyRole.MANAGER);
    }
}
