package com.recaring.care.business;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipManager;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.care.implement.CareRelationshipWriter;
import com.recaring.care.vo.CaregiverInfo;
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
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareRelationshipService 단위 테스트")
class CareRelationshipServiceTest {

    @InjectMocks
    private CareRelationshipService careRelationshipService;

    @Mock
    private CareRelationshipManager careRelationshipManager;

    @Mock
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private CareRelationshipWriter careRelationshipWriter;

    @Mock
    private CareRelationshipValidator careRelationshipValidator;

    @Test
    @DisplayName("보호자/관리자 목록 조회 시 접근 권한 검증 후 결과를 반환한다")
    void getCaregivers_validates_then_returns_result() {
        // given
        List<CaregiverInfo> expected = List.of(
                CareFixture.createCaregiverInfo(CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)
        );
        given(careRelationshipReader.findCaregiverInfos(CareFixture.WARD_MEMBER_KEY)).willReturn(expected);

        List<CaregiverInfo> result = careRelationshipService.getCaregivers(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
        then(careRelationshipValidator).should(times(1))
                .validateCaregiverViewAccess(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipReader).should(times(1)).findCaregiverInfos(CareFixture.WARD_MEMBER_KEY);
    }

    @Test
    @DisplayName("보호자/관리자 목록 조회 시 권한이 없으면 예외가 전파된다")
    void getCaregivers_propagates_exception_when_unauthorized() {
        willThrow(new AppException(ErrorType.NOT_GUARDIAN_OF_WARD))
                .given(careRelationshipValidator)
                .validateCaregiverViewAccess(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() ->
                careRelationshipService.getCaregivers(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(careRelationshipReader).should(times(0)).findCaregiverInfos(any());
    }

    // ── removeWard ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("보호 대상자 케어 관계 삭제 - 검증 통과 후 Writer의 delete가 호출된다")
    void removeWard_validates_then_deletes() {
        careRelationshipService.removeWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        then(careRelationshipValidator).should(times(1))
                .validateCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipManager).should(times(1))
                .leaveCare(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
    }

    @Test
    @DisplayName("보호 대상자 케어 관계 삭제 - 케어 관계가 없으면 예외가 전파된다")
    void removeWard_propagates_exception_when_not_caregiver() {
        willThrow(new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP))
                .given(careRelationshipValidator)
                .validateCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() ->
                careRelationshipService.removeWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);

        then(careRelationshipManager).should(times(0)).leaveCare(any(), any());
    }

    // ── removeCaregiver ────────────────────────────────────────────────────

    @Test
    @DisplayName("보호자/관계자 케어 관계 삭제 - 주보호자 검증 후 Writer의 delete가 호출된다")
    void removeCaregiver_validates_primary_guardian_role_then_deletes() {
        careRelationshipService.removeCaregiver(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);

        then(careRelationshipValidator).should(times(1))
                .validatePrimaryGuardianRole(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipValidator).should(times(1))
                .validateCaregiverRemovable(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);
        then(careRelationshipWriter).should(times(1))
                .delete(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);
    }

    @Test
    @DisplayName("보호자/관계자 케어 관계 삭제 - 요청자가 주보호자가 아니면 예외가 전파된다")
    void removeCaregiver_propagates_exception_when_not_primary_guardian_role() {
        willThrow(new AppException(ErrorType.NOT_PRIMARY_GUARDIAN_ROLE_IN_CARE))
                .given(careRelationshipValidator)
                .validatePrimaryGuardianRole(CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() ->
                careRelationshipService.removeCaregiver(
                        CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_PRIMARY_GUARDIAN_ROLE_IN_CARE);

        then(careRelationshipWriter).should(times(0)).delete(any(), any());
    }

    @Test
    @DisplayName("보호자/관계자 케어 관계 삭제 - 대상이 주보호자면 예외가 전파되고 삭제하지 않는다")
    void removeCaregiver_propagates_exception_when_target_is_primary_guardian() {
        willThrow(new AppException(ErrorType.CANNOT_REMOVE_PRIMARY_GUARDIAN))
                .given(careRelationshipValidator)
                .validateCaregiverRemovable(CareFixture.WARD_MEMBER_KEY, "other-primary-key");

        assertThatThrownBy(() ->
                careRelationshipService.removeCaregiver(
                        CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "other-primary-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CANNOT_REMOVE_PRIMARY_GUARDIAN);

        then(careRelationshipWriter).should(times(0)).delete(any(), any());
    }

    // ── updateWardNickname ─────────────────────────────────────────────────

    @Test
    @DisplayName("별명 수정 - 앞뒤 공백을 제거해 저장한다")
    void updateWardNickname_trims_surrounding_whitespace() {
        careRelationshipService.updateWardNickname(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "  할머니  ");

        then(careRelationshipValidator).should(times(1))
                .validateCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipWriter).should(times(1))
                .updateWardNickname(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, "할머니");
    }

    @Test
    @DisplayName("별명 수정 - 공백뿐이면 null로 저장해 별명을 해제한다")
    void updateWardNickname_clears_when_blank() {
        careRelationshipService.updateWardNickname(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "   ");

        then(careRelationshipWriter).should(times(1))
                .updateWardNickname(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY, null);
    }

    @Test
    @DisplayName("별명 수정 - 케어 관계가 없으면 예외가 전파되고 저장하지 않는다")
    void updateWardNickname_propagates_exception_when_not_caregiver() {
        willThrow(new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP))
                .given(careRelationshipValidator)
                .validateCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() -> careRelationshipService.updateWardNickname(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, "할머니"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);

        then(careRelationshipWriter).should(times(0)).updateWardNickname(any(), any(), any());
    }

    // ── updateCaregiverRole ────────────────────────────────────────────────

    @Test
    @DisplayName("관계 수정 - 주보호자 검증과 역할 검증을 통과해야 Writer가 호출된다")
    void updateCaregiverRole_validates_then_updates() {
        careRelationshipService.updateCaregiverRole(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN);

        then(careRelationshipValidator).should(times(1))
                .validatePrimaryGuardianRole(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipValidator).should(times(1))
                .validateCareRoleChange(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);
        then(careRelationshipWriter).should(times(1))
                .updateCareRole(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY, CareRole.GUARDIAN);
    }

    @Test
    @DisplayName("관계 수정 - 요청자가 주보호자가 아니면 예외가 전파되고 저장하지 않는다")
    void updateCaregiverRole_propagates_exception_when_not_primary_guardian() {
        willThrow(new AppException(ErrorType.NOT_PRIMARY_GUARDIAN_ROLE_IN_CARE))
                .given(careRelationshipValidator)
                .validatePrimaryGuardianRole(CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() -> careRelationshipService.updateCaregiverRole(
                CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY,
                CareFixture.GUARDIAN_MEMBER_KEY, CareRole.MANAGER))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_PRIMARY_GUARDIAN_ROLE_IN_CARE);

        then(careRelationshipWriter).should(times(0)).updateCareRole(any(), any(), any());
    }
}
