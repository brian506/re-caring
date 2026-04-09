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
import static org.mockito.BDDMockito.given;

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
    @DisplayName("보호 대상자 추가 검증 - 이미 1명의 보호 대상자가 있으면 예외가 발생한다")
    void validateCanAddWard_fails_when_limit_exceeded() {
        CareRelationship existing = CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(existing));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, "another-ward-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("보호 대상자 추가 검증 - 이미 동일한 대상자가 있으면 예외가 발생한다")
    void validateCanAddWard_fails_when_duplicated() {
        CareRelationship existing = CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
        given(careRelationshipRepository.findAllByCaregiverMemberKey(CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(List.of(existing));

        // 이미 등록된 wardMemberKey로 재등록 시도
        // limit 체크가 먼저 걸리므로 CARE_CAREGIVER_LIMIT_EXCEEDED 발생
        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    // ── validateCanAddManager ──────────────────────────────────────────────

    @Test
    @DisplayName("관리자 추가 검증 - 관리자가 없으면 정상 통과한다")
    void validateCanAddManager_success() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of());

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("관리자 추가 검증 - 관리자가 3명이면 예외가 발생한다")
    void validateCanAddManager_fails_when_limit_exceeded() {
        List<CareRelationship> existingManagers = List.of(
                CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-1"),
                CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-2"),
                CareFixture.createManagerRelationship(CareFixture.WARD_MEMBER_KEY, "manager-3")
        );
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(existingManagers);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("관리자 추가 검증 - 동일한 관리자가 이미 존재하면 예외가 발생한다")
    void validateCanAddManager_fails_when_duplicated() {
        CareRelationship existing = CareFixture.createManagerRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(existing));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddManager(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.ALREADY_CARE_RELATIONSHIP);
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
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
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
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                CareFixture.WARD_MEMBER_KEY, "stranger-key", CareRole.GUARDIAN))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        "stranger-key", CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);
    }
}
