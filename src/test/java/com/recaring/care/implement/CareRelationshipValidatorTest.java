package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.vo.CareRole;
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
    @DisplayName("보호 대상자 추가 검증 - 보호자로 맡은 대상자가 5명이면 CARE_WARD_LIMIT_EXCEEDED")
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

    @Test
    @DisplayName("보호 대상자 추가 검증 - 대상자에 이미 4명이 연결돼 있으면 한 명 더 연결할 수 있다")
    void validateCanAddWard_passes_when_ward_has_one_below_caregiver_limit() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-1"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-2"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-3"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-4")));

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호 대상자 추가 검증 - 대상자에 이미 5명이 연결돼 있으면 CARE_CAREGIVER_LIMIT_EXCEEDED")
    void validateCanAddWard_fails_when_ward_reached_caregiver_limit() {
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-1"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-2"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-3"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-4"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-5")));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddWard(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    // ── validateCanAddCaregiver ────────────────────────────────────────────

    @Test
    @DisplayName("케어 관계 추가 검증 - 해당 대상자의 보호자가 요청하면 정상 통과한다")
    void validateCanAddCaregiver_success() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(CareFixture.createGuardianRelationship(
                        CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY)));

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.SECOND_GUARDIAN_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 요청자가 해당 대상자와 관계가 없으면 NOT_FOUND_CARE_RELATIONSHIP")
    void validateCanAddCaregiver_fails_when_requester_is_not_related() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, "stranger-key"))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        "stranger-key", CareFixture.WARD_MEMBER_KEY, CareFixture.SECOND_GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);

        then(careRelationshipRepository).should(times(0)).findAllByWardMemberKey(anyString());
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 대상자에 연결된 보호자가 4명이면 한 명 더 추가할 수 있다")
    void validateCanAddCaregiver_passes_at_one_below_limit() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-3"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-4"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-5")));

        assertThatCode(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.SECOND_GUARDIAN_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 대상자에 연결된 보호자가 5명이면 CARE_CAREGIVER_LIMIT_EXCEEDED")
    void validateCanAddCaregiver_fails_when_limit_exceeded() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-2"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-3"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-4"),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, "guardian-5")));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.SECOND_GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("케어 관계 추가 검증 - 이미 연결된 사람을 다시 추가하면 예외가 발생한다")
    void validateCanAddCaregiver_fails_when_duplicated() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(true);
        given(careRelationshipRepository.findAllByWardMemberKey(CareFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY),
                        CareFixture.createGuardianRelationship(CareFixture.WARD_MEMBER_KEY, CareFixture.SECOND_GUARDIAN_MEMBER_KEY)));

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCanAddCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.SECOND_GUARDIAN_MEMBER_KEY))
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
    @DisplayName("보호자 목록 조회 권한 - 해당 대상자의 보호자는 접근 가능하다")
    void validateCaregiverViewAccess_success_when_guardian() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(true);

        assertThatCode(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("보호자 목록 조회 권한 - 관계없는 사람은 NOT_FOUND_CARE_RELATIONSHIP")
    void validateCaregiverViewAccess_fails_when_unauthorized() {
        given(careRelationshipRepository.existsCareRelationship(
                CareFixture.WARD_MEMBER_KEY, "stranger-key"))
                .willReturn(false);

        assertThatThrownBy(() ->
                careRelationshipValidator.validateCaregiverViewAccess(
                        "stranger-key", CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
    }
}
