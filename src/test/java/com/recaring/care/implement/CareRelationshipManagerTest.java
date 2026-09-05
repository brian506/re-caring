package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareRelationshipManager 단위 테스트")
class CareRelationshipManagerTest {

    private static final String WARD_KEY = CareFixture.WARD_MEMBER_KEY;
    private static final String OTHER_WARD_KEY = "other-ward-key";
    private static final String PRIMARY_KEY = "primary-guardian-key";
    private static final String OLDER_GUARDIAN_KEY = "older-guardian-key";
    private static final String NEWER_GUARDIAN_KEY = "newer-guardian-key";
    private static final String OLDER_MANAGER_KEY = "older-manager-key";
    private static final String NEWER_MANAGER_KEY = "newer-manager-key";

    @InjectMocks
    private CareRelationshipManager careRelationshipManager;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private CareInvitationWriter careInvitationWriter;

    @Test
    @DisplayName("주보호자가 떠나면 남은 보호자 중 먼저 등록된 사람이 주보호자가 된다")
    void leaveCare_promotes_the_oldest_remaining_guardian() {
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        CareRelationship newerGuardian = CareFixture.createRelationship(WARD_KEY, NEWER_GUARDIAN_KEY, CareRole.GUARDIAN, 3L);
        CareRelationship olderGuardian = CareFixture.createRelationship(WARD_KEY, OLDER_GUARDIAN_KEY, CareRole.GUARDIAN, 2L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY))
                .willReturn(List.of(primary, newerGuardian, olderGuardian));

        careRelationshipManager.leaveCare(WARD_KEY, PRIMARY_KEY);

        assertThat(olderGuardian.getCareRole()).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(newerGuardian.getCareRole()).isEqualTo(CareRole.GUARDIAN);
        then(careRelationshipRepository).should().delete(primary);
    }

    @Test
    @DisplayName("주보호자가 떠날 때 보호자가 없으면 남은 관계자 중 먼저 등록된 사람이 주보호자가 된다")
    void leaveCare_promotes_the_oldest_remaining_manager_without_guardian() {
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        CareRelationship newerManager = CareFixture.createRelationship(WARD_KEY, NEWER_MANAGER_KEY, CareRole.MANAGER, 3L);
        CareRelationship olderManager = CareFixture.createRelationship(WARD_KEY, OLDER_MANAGER_KEY, CareRole.MANAGER, 2L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY))
                .willReturn(List.of(primary, newerManager, olderManager));

        careRelationshipManager.leaveCare(WARD_KEY, PRIMARY_KEY);

        assertThat(olderManager.getCareRole()).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(newerManager.getCareRole()).isEqualTo(CareRole.MANAGER);
    }

    @Test
    @DisplayName("보호자가 관계자보다 늦게 등록됐어도 보호자가 먼저 주보호자가 된다")
    void leaveCare_prefers_guardian_over_earlier_manager() {
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        CareRelationship manager = CareFixture.createRelationship(WARD_KEY, OLDER_MANAGER_KEY, CareRole.MANAGER, 2L);
        CareRelationship guardian = CareFixture.createRelationship(WARD_KEY, NEWER_GUARDIAN_KEY, CareRole.GUARDIAN, 3L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY))
                .willReturn(List.of(primary, manager, guardian));

        careRelationshipManager.leaveCare(WARD_KEY, PRIMARY_KEY);

        assertThat(guardian.getCareRole()).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(manager.getCareRole()).isEqualTo(CareRole.MANAGER);
    }

    @Test
    @DisplayName("주보호자가 둘일 때 하나가 떠나면 승계는 일어나지 않는다")
    void leaveCare_does_not_promote_while_another_primary_guardian_remains() {
        CareRelationship leavingPrimary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        CareRelationship remainingPrimary = CareFixture.createRelationship(WARD_KEY, "another-primary-key", CareRole.PRIMARY_GUARDIAN, 2L);
        CareRelationship guardian = CareFixture.createRelationship(WARD_KEY, OLDER_GUARDIAN_KEY, CareRole.GUARDIAN, 3L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY))
                .willReturn(List.of(leavingPrimary, remainingPrimary, guardian));

        careRelationshipManager.leaveCare(WARD_KEY, PRIMARY_KEY);

        assertThat(guardian.getCareRole()).isEqualTo(CareRole.GUARDIAN);
        assertThat(remainingPrimary.getCareRole()).isEqualTo(CareRole.PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("보호자가 떠나면 남은 사람들의 관계는 그대로다")
    void leaveCare_keeps_roles_when_a_non_primary_leaves() {
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        CareRelationship guardian = CareFixture.createRelationship(WARD_KEY, OLDER_GUARDIAN_KEY, CareRole.GUARDIAN, 2L);
        CareRelationship manager = CareFixture.createRelationship(WARD_KEY, OLDER_MANAGER_KEY, CareRole.MANAGER, 3L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY))
                .willReturn(List.of(primary, guardian, manager));

        careRelationshipManager.leaveCare(WARD_KEY, OLDER_GUARDIAN_KEY);

        assertThat(primary.getCareRole()).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(manager.getCareRole()).isEqualTo(CareRole.MANAGER);
        then(careRelationshipRepository).should().delete(guardian);
    }

    @Test
    @DisplayName("마지막 케어 관계가 끊기면 그 보호 대상자의 대기 중 초대를 정리한다")
    void leaveCare_clears_pending_invitations_when_no_relationship_remains() {
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY)).willReturn(List.of(primary));

        careRelationshipManager.leaveCare(WARD_KEY, PRIMARY_KEY);

        then(careInvitationWriter).should().deleteAllByWardMemberKey(WARD_KEY);
    }

    @Test
    @DisplayName("케어 관계가 남아 있으면 대기 중 초대를 정리하지 않는다")
    void leaveCare_keeps_pending_invitations_while_a_relationship_remains() {
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        CareRelationship guardian = CareFixture.createRelationship(WARD_KEY, OLDER_GUARDIAN_KEY, CareRole.GUARDIAN, 2L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY)).willReturn(List.of(primary, guardian));

        careRelationshipManager.leaveCare(WARD_KEY, PRIMARY_KEY);

        then(careInvitationWriter).should(never()).deleteAllByWardMemberKey(anyString());
    }

    @Test
    @DisplayName("케어 관계가 없는 사람이 이탈하려 하면 예외가 발생한다")
    void leaveCare_fails_when_relationship_not_found() {
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY)).willReturn(List.of(primary));

        assertThatThrownBy(() -> careRelationshipManager.leaveCare(WARD_KEY, "stranger-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
    }

    @Test
    @DisplayName("탈퇴하면 그 회원이 주보호자였던 모든 보호 대상자에 승계가 일어난다")
    void leaveAllCare_promotes_a_successor_for_every_ward() {
        given(careRelationshipRepository.findAllByCaregiverMemberKey(PRIMARY_KEY)).willReturn(List.of(
                CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L),
                CareFixture.createRelationship(OTHER_WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 2L)));
        CareRelationship guardian = CareFixture.createRelationship(WARD_KEY, OLDER_GUARDIAN_KEY, CareRole.GUARDIAN, 3L);
        CareRelationship manager = CareFixture.createRelationship(OTHER_WARD_KEY, OLDER_MANAGER_KEY, CareRole.MANAGER, 4L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY)).willReturn(List.of(guardian));
        given(careRelationshipRepository.findAllByWardMemberKey(OTHER_WARD_KEY)).willReturn(List.of(manager));

        careRelationshipManager.leaveAllCare(PRIMARY_KEY);

        assertThat(guardian.getCareRole()).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        assertThat(manager.getCareRole()).isEqualTo(CareRole.PRIMARY_GUARDIAN);
        then(careRelationshipRepository).should().deleteAllByMemberKey(PRIMARY_KEY);
    }

    @Test
    @DisplayName("탈퇴한 회원이 주보호자가 아니던 보호 대상자에는 승계가 일어나지 않는다")
    void leaveAllCare_does_not_promote_when_the_ward_still_has_a_primary_guardian() {
        given(careRelationshipRepository.findAllByCaregiverMemberKey(OLDER_MANAGER_KEY)).willReturn(List.of(
                CareFixture.createRelationship(WARD_KEY, OLDER_MANAGER_KEY, CareRole.MANAGER, 2L)));
        CareRelationship primary = CareFixture.createRelationship(WARD_KEY, PRIMARY_KEY, CareRole.PRIMARY_GUARDIAN, 1L);
        CareRelationship guardian = CareFixture.createRelationship(WARD_KEY, OLDER_GUARDIAN_KEY, CareRole.GUARDIAN, 3L);
        given(careRelationshipRepository.findAllByWardMemberKey(WARD_KEY)).willReturn(List.of(primary, guardian));

        careRelationshipManager.leaveAllCare(OLDER_MANAGER_KEY);

        assertThat(guardian.getCareRole()).isEqualTo(CareRole.GUARDIAN);
        then(careInvitationWriter).should(never()).deleteAllByWardMemberKey(anyString());
    }
}
