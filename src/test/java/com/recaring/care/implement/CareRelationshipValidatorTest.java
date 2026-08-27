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
    @DisplayName("보호 대상자 추가 검증 - 이미 5명의 보호 대상자가 있으면 예외가 발생한다")
    void validateCanAddWard_fails_when_limit_exceeded() {
        List<CareRelationship> existing = List.of(
                CareFixture.createGuardianRelationship("ward-key-1", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createGuardianRelationship("ward-key-2", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createGuardianRelationship("ward-key-3", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createGuardianRelationship("ward-key-4", CareFixture.GUARDIAN_MEMBER_KEY),
                CareFixture.createGuardianRelationship("ward-key-5", CareFixture.GUARDIAN_MEMBER_KEY));
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

    // ── validateCanAddManager ──────────────────────────────────────────────

    @Test
    @DisplayName("관리자 추가 검증 - 관리자가 없으면 정상 통과한다")
    void validateCanAddManager_success() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of());

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관리자 추가 검증 - 요청자가 해당 대상자의 보호자가 아니면 예외가 발생한다")
    void validateCanAddManager_fails_when_requester_not_guardian() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "another-manager-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_ROLE_IN_CARE);

        then(careRelationshipRepository).should(times(0)).findAllByWardMemberKey(anyString());
    }

    @Test
    @DisplayName("관리자 추가 검증 - 관리자가 3명이면 예외가 발생한다")
    void validateCanAddManager_fails_when_limit_exceeded() {
        List<CareRelationship> existingManagers = List.of(
                CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-1"),
                CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-2"),
                CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-3")
        );
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(existingManagers);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("관리자 추가 검증 - 관리자가 2명이면 한 명 더 추가할 수 있다")
    void validateCanAddManager_passes_at_one_below_limit() {
        // Given
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-1"),
                        CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-2")));

        // When / Then
        assertThatCode(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관리자 추가 검증 - 동일한 관리자가 이미 존재하면 예외가 발생한다")
    void validateCanAddManager_fails_when_duplicated() {
        CareRelationship existing = CareFixture.createManagerRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(existing));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_CARE_RELATIONSHIP);
    }

    // ── validateCanAddGuardian ───────────────────────────────────────────

    @Test
    @DisplayName("공동 보호자 추가 검증 - 공동 보호자가 없으면 정상 통과한다")
    void validateCanAddGuardian_success() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of());

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddGuardian(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "another-guardian-key"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공동 보호자 추가 검증 - 요청자가 해당 대상자의 보호자가 아니면 예외가 발생한다")
    void validateCanAddGuardian_fails_when_requester_not_guardian() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddGuardian(
                        CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "another-guardian-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_ROLE_IN_CARE);

        then(careRelationshipRepository).should(times(0)).findAllByWardMemberKey(anyString());
    }

    @Test
    @DisplayName("공동 보호자 추가 검증 - 이미 공동 보호자가 있으면 예외가 발생한다")
    void validateCanAddGuardian_fails_when_limit_exceeded() {
        CareRelationship existing = CareFixture.createGuardianRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(existing));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddGuardian(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "another-guardian-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
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

    // ── validateGuardianRole ─────────────────────────────────────────────

    @Test
    @DisplayName("보호자 역할 검증 - CareRole이 GUARDIAN이면 정상 통과한다")
    void validateGuardianRole_success() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);

        assertThatCode(() ->
                careRelationshipValidator.validateGuardianRole(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호자 역할 검증 - CareRole이 GUARDIAN이 아니면 예외가 발생한다")
    void validateGuardianRole_fails_when_not_guardian_role() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateGuardianRole(
                        CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_ROLE_IN_CARE);
    }

    // ── validateCaregiverViewAccess ────────────────────────────────────────

    @Test
    @DisplayName("보호자 목록 조회 권한 - 본인(보호 대상자)은 접근 가능하다")
    void validateCaregiverViewAccess_success_when_ward_self() {
        // wardKey == requesterKey 이면 본인 접근
        assertThatCode(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호자 목록 조회 권한 - 해당 ward의 보호자(GUARDIAN)는 접근 가능하다")
    void validateCaregiverViewAccess_success_when_guardian() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN))
                .willReturn(true);

        assertThatCode(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호자 목록 조회 권한 - 관계없는 사람은 예외가 발생한다")
    void validateCaregiverViewAccess_fails_when_unauthorized() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, "stranger-key", CareRole.GUARDIAN))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        "stranger-key", CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);
    }
}
