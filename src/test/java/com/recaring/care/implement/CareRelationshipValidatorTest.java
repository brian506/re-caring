package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.member.implement.MemberValidator;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareRelationshipValidator 단위 테스트")
class CareRelationshipValidatorTest {

    @InjectMocks
    private CareRelationshipValidator careRelationshipValidator;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private MemberValidator memberValidator;

    // ── validateCanAddWard ──────────────────────────────────────────────────

    @Test
    @DisplayName("보호 대상자 추가 검증 - 관계가 없으면 정상 통과한다")
    void validateCanAddWard_success() {
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of());

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호 대상자 추가 검증 - 주보호자 4명과 보호자 1명을 합쳐 5명이면 예외가 발생한다")
    void validateCanAddWard_fails_when_limit_exceeded() {
        List<CareRelationship> existing = List.of(
                CareFixture.createGuardianRelationship("ward-key-1", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createGuardianRelationship("ward-key-2", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createGuardianRelationship("ward-key-3", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createGuardianRelationship("ward-key-4", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createPrimaryGuardianRelationship("ward-key-5", CareFixture.GUARDIAN_MEMBER_KEY));
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(existing);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, "another-ward-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_WARD_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("보호 대상자 추가 검증 - 보호 대상자가 4명이면 한 명 더 추가할 수 있다")
    void validateCanAddWard_passes_at_one_below_limit() {
        // Given
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship("ward-key-1", CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createGuardianRelationship("ward-key-2", CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createGuardianRelationship("ward-key-3", CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createGuardianRelationship("ward-key-4", CareFixture.GUARDIAN_MEMBER_KEY)));

        // When / Then
        assertThatCode(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, "another-ward-key"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호 대상자 추가 검증 - 이미 동일한 대상자가 있으면 예외가 발생한다")
    void validateCanAddWard_fails_when_duplicated() {
        CareRelationship existing = CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(existing));

        // 이미 등록된 wardMemberKey로 재등록 시도
        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_CARE_RELATIONSHIP);
    }

    // ── validateCanAddCaregiver ────────────────────────────────────────────

    @Test
    @DisplayName("케어 관계 추가 검증 - 주보호자가 요청하면 정상 통과한다")
    void validateCanAddCaregiver_success() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.PRIMARY_GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createPrimaryGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 요청자가 해당 대상자의 주보호자가 아니면 예외가 발생한다")
    void validateCanAddCaregiver_fails_when_requester_is_not_primary_guardian() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.PRIMARY_GUARDIAN))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "another-manager-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_PRIMARY_GUARDIAN_ROLE_IN_CARE);

        then(careRelationshipRepository).should(times(0)).findAllByWardMemberKey(anyString());
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 대상자에 연결된 사람이 4명이면 한 명 더 추가할 수 있다")
    void validateCanAddCaregiver_passes_at_one_below_limit() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.PRIMARY_GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-1"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-2"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-3")));

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 역할이 섞여 5명이면 예외가 발생한다")
    void validateCanAddCaregiver_fails_when_limit_exceeded() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.PRIMARY_GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-2"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-1"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-2"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-3")));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 한도는 역할별로 세지 않는다. 보호자 자리가 비어 있어도 총원이 5명이면 막는다")
    void validateCanAddCaregiver_counts_every_role_together() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.PRIMARY_GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-1"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-2"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-3"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-4")));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "new-guardian-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 이미 연결된 사람을 다시 추가하면 예외가 발생한다")
    void validateCanAddCaregiver_fails_when_duplicated() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.PRIMARY_GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_CARE_RELATIONSHIP);
    }


    // ── validateCaregiver ────────────────────────────────────────────────

    @Test
    @DisplayName("케어 관계 보호자 검증 - 케어 관계가 존재하면 정상 통과한다")
    void validateCaregiver_success() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(true);

        assertThatCode(() ->
                careRelationshipValidator.validateCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("케어 관계 보호자 검증 - 케어 관계가 없으면 예외가 발생한다")
    void validateCaregiver_fails_when_relationship_not_found() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, "stranger-key"))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCaregiver(
                        "stranger-key", CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
    }

    // ── validateCaregiverViewAccess ────────────────────────────────────────

    @Test
    @DisplayName("보호자 목록 조회 권한 - 본인(보호 대상자)은 접근 가능하다")
    void validateCaregiverViewAccess_success_when_ward_self() {
        assertThatCode(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호자 목록 조회 권한 - 해당 대상자의 보호자 계열은 접근 가능하다")
    void validateCaregiverViewAccess_success_when_guardian() {
        given(careRelationshipRepository.existsCareRelationshipInRoles(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.guardianRoles()))
                .willReturn(true);

        assertThatCode(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호자 목록 조회 권한 - 관계없는 사람은 예외가 발생한다")
    void validateCaregiverViewAccess_fails_when_unauthorized() {
        given(careRelationshipRepository.existsCareRelationshipInRoles(
                CareFixture.WARD_MEMBER_KEY, "stranger-key", CareRole.guardianRoles()))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        "stranger-key", CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);
    }

    // ── validateCareRoleChange ─────────────────────────────────────────────

    @Test
    @DisplayName("관계 수정 검증 - 보호자의 관계는 바꿀 수 있다")
    void validateCareRoleChange_allows_guardian() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "primary-key"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));

        assertThatCode(() ->
                careRelationshipValidator.validateCareRoleChange(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관계 수정 검증 - 관계자의 관계는 바꿀 수 있다")
    void validateCareRoleChange_allows_manager() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "primary-key"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY)));

        assertThatCode(() ->
                careRelationshipValidator.validateCareRoleChange(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관계 수정 검증 - 대상자에 연결된 사람이 5명으로 꽉 차 있어도 역할 변경은 통과한다")
    void validateCareRoleChange_ignores_caregiver_limit() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "primary-key"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-2"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-3"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-4")));

        assertThatCode(() ->
                careRelationshipValidator.validateCareRoleChange(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관계 수정 검증 - 주보호자의 관계는 바꿀 수 없다")
    void validateCareRoleChange_fails_when_target_is_primary_guardian() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createPrimaryGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCareRoleChange(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CANNOT_CHANGE_PRIMARY_GUARDIAN_ROLE);
    }

    @Test
    @DisplayName("관계 수정 검증 - 케어 관계에 없는 사람이면 예외가 발생한다")
    void validateCareRoleChange_fails_when_relationship_not_found() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createPrimaryGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCareRoleChange(
                        CareFixture.WARD_MEMBER_KEY, "stranger-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
    }

    // ── validateCaregiverRemovable ─────────────────────────────────────────

    @Test
    @DisplayName("케어 관계 삭제 검증 - 보호자는 삭제할 수 있다")
    void validateCaregiverRemovable_allows_guardian() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "primary-key"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));

        assertThatCode(() ->
                careRelationshipValidator.validateCaregiverRemovable(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("케어 관계 삭제 검증 - 주보호자는 삭제할 수 없다")
    void validateCaregiverRemovable_fails_when_target_is_primary_guardian() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "primary-key"),
                        CareFixture.createPrimaryGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCaregiverRemovable(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CANNOT_REMOVE_PRIMARY_GUARDIAN);
    }

    @Test
    @DisplayName("케어 관계 삭제 검증 - 케어 관계에 없는 사람이면 예외가 발생한다")
    void validateCaregiverRemovable_fails_when_relationship_not_found() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createPrimaryGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, "primary-key")));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCaregiverRemovable(
                        CareFixture.WARD_MEMBER_KEY, "stranger-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
    }
}
