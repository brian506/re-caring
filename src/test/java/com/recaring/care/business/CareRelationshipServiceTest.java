package com.recaring.care.business;

import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.care.implement.CareRelationshipWriter;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.vo.WardInfo;
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
    private CareRelationshipReader careRelationshipReader;

    @Mock
    private CareRelationshipWriter careRelationshipWriter;

    @Mock
    private CareRelationshipValidator careRelationshipValidator;

    @Test
    @DisplayName("내 보호 대상자 목록 조회 시 Reader에서 조립된 결과를 반환한다")
    void getMyWards_returns_reader_result() {
        List<WardInfo> expected = List.of(
                new WardInfo(CareFixture.WARD_MEMBER_KEY, "보호대상자", CareFixture.WARD_PHONE, com.recaring.care.dataaccess.entity.CareRole.GUARDIAN)
        );
        given(careRelationshipReader.findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY)).willReturn(expected);

        List<WardInfo> result = careRelationshipService.getMyWards(CareFixture.GUARDIAN_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
        then(careRelationshipReader).should(times(1)).findWardInfos(CareFixture.GUARDIAN_MEMBER_KEY);
    }

    @Test
    @DisplayName("보호자/관리자 목록 조회 시 접근 권한 검증 후 결과를 반환한다")
    void getCaregivers_validates_then_returns_result() {
        List<CaregiverInfo> expected = List.of(
                new CaregiverInfo(CareFixture.GUARDIAN_MEMBER_KEY, "보호자", CareFixture.GUARDIAN_PHONE, com.recaring.care.dataaccess.entity.CareRole.GUARDIAN)
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
                .validateIsCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipWriter).should(times(1))
                .delete(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
    }

    @Test
    @DisplayName("보호 대상자 케어 관계 삭제 - 케어 관계가 없으면 예외가 전파된다")
    void removeWard_propagates_exception_when_not_caregiver() {
        willThrow(new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP))
                .given(careRelationshipValidator)
                .validateIsCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() ->
                careRelationshipService.removeWard(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);

        then(careRelationshipWriter).should(times(0)).delete(any(), any());
    }

    // ── removeCaregiver ────────────────────────────────────────────────────

    @Test
    @DisplayName("보호자/관계자 케어 관계 삭제 - GUARDIAN 역할 검증 후 Writer의 delete가 호출된다")
    void removeCaregiver_validates_guardian_role_then_deletes() {
        careRelationshipService.removeCaregiver(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);

        then(careRelationshipValidator).should(times(1))
                .validateIsGuardianRole(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipWriter).should(times(1))
                .delete(CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY);
    }

    @Test
    @DisplayName("보호자/관계자 케어 관계 삭제 - 요청자가 GUARDIAN 역할이 아니면 예외가 전파된다")
    void removeCaregiver_propagates_exception_when_not_guardian_role() {
        willThrow(new AppException(ErrorType.NOT_GUARDIAN_ROLE_IN_CARE))
                .given(careRelationshipValidator)
                .validateIsGuardianRole(CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() ->
                careRelationshipService.removeCaregiver(
                        CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_ROLE_IN_CARE);

        then(careRelationshipWriter).should(times(0)).delete(any(), any());
    }
}
