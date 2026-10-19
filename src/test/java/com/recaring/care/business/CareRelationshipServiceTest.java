package com.recaring.care.business;

import com.recaring.care.vo.CareRole;
import com.recaring.care.fixture.CareFixture;
import com.recaring.care.implement.CareRelationshipManager;
import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.care.implement.CareRelationshipValidator;
import com.recaring.care.implement.CareRelationshipWriter;
import com.recaring.care.implement.DesignatedAvatarManager;
import com.recaring.care.vo.CaregiverInfo;
import com.recaring.member.vo.ProfileAvatarCode;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private DesignatedAvatarManager designatedAvatarManager;

    @Test
    @DisplayName("보호자/관리자 목록 조회 시 접근 권한 검증 후 결과를 반환한다")
    void getCaregivers_validates_then_returns_result() {
        // given
        List<CaregiverInfo> expected = List.of(
                CareFixture.createCaregiverInfo(CareFixture.GUARDIAN_MEMBER_KEY, CareRole.GUARDIAN)
        );
        given(careRelationshipReader.findCaregiverInfos(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY))
                .willReturn(expected);

        List<CaregiverInfo> result = careRelationshipService.getCaregivers(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);

        assertThat(result).isEqualTo(expected);
        then(careRelationshipValidator).should(times(1))
                .validateCaregiverViewAccess(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(careRelationshipReader).should(times(1))
                .findCaregiverInfos(CareFixture.WARD_MEMBER_KEY, CareFixture.GUARDIAN_MEMBER_KEY);
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

        then(careRelationshipReader).should(times(0)).findCaregiverInfos(any(), any());
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

    @Test
    @DisplayName("대상자 얼굴 지정 - 케어 관계 검증 후 요청자 범위로 대상자 본인에게 저장한다")
    void designateWardAvatar_validates_then_designates_in_requester_scope() {
        careRelationshipService.designateWardAvatar(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.SENIOR_AVATAR_CODE);

        then(careRelationshipValidator).should(times(1))
                .validateCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        then(designatedAvatarManager).should(times(1)).designate(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY,
                new ProfileAvatarCode(CareFixture.SENIOR_AVATAR_CODE));
    }

    @Test
    @DisplayName("대상자 얼굴 지정 - 허용 코드 외 값이면 INVALID_PROFILE_AVATAR_CODE이고 저장하지 않는다")
    void designateWardAvatar_rejects_unknown_code() {
        assertThatThrownBy(() -> careRelationshipService.designateWardAvatar(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.UNKNOWN_AVATAR_CODE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PROFILE_AVATAR_CODE);

        then(designatedAvatarManager).should(times(0)).designate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("대상자 얼굴 지정 - 케어 관계가 없으면 예외가 전파되고 저장하지 않는다")
    void designateWardAvatar_propagates_exception_when_not_caregiver() {
        willThrow(new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP))
                .given(careRelationshipValidator)
                .validateCaregiver(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() -> careRelationshipService.designateWardAvatar(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.SENIOR_AVATAR_CODE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);

        then(designatedAvatarManager).should(times(0)).designate(any(), any(), any(), any());
    }

    // ── designateCaregiverAvatar ───────────────────────────────────────────

    @Test
    @DisplayName("보호자 얼굴 지정 - 요청자의 열람 권한을 먼저 검증하고, 대상의 케어 관계를 확인한 뒤 요청자 범위로 저장한다")
    void designateCaregiverAvatar_validates_both_sides_then_designates() {
        careRelationshipService.designateCaregiverAvatar(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY,
                CareFixture.MANAGER_MEMBER_KEY, CareFixture.ADULT_AVATAR_CODE);

        InOrder inOrder = inOrder(careRelationshipValidator, designatedAvatarManager);
        inOrder.verify(careRelationshipValidator)
                .validateCaregiverViewAccess(CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        inOrder.verify(careRelationshipValidator)
                .validateCaregiver(CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);
        inOrder.verify(designatedAvatarManager).designate(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY, CareFixture.MANAGER_MEMBER_KEY,
                new ProfileAvatarCode(CareFixture.ADULT_AVATAR_CODE));
    }

    @Test
    @DisplayName("보호자 얼굴 지정 - 요청자에게 열람 권한이 없으면 예외가 전파되고 저장하지 않는다")
    void designateCaregiverAvatar_propagates_exception_when_requester_unauthorized() {
        willThrow(new AppException(ErrorType.NOT_GUARDIAN_OF_WARD))
                .given(careRelationshipValidator)
                .validateCaregiverViewAccess(CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() -> careRelationshipService.designateCaregiverAvatar(
                CareFixture.MANAGER_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY,
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.ADULT_AVATAR_CODE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_GUARDIAN_OF_WARD);

        then(designatedAvatarManager).should(times(0)).designate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("보호자 얼굴 지정 - 대상이 케어 관계에 없으면 예외가 전파되고 저장하지 않는다")
    void designateCaregiverAvatar_propagates_exception_when_target_not_in_care() {
        willThrow(new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP))
                .given(careRelationshipValidator)
                .validateCaregiver("stranger-key", CareFixture.WARD_MEMBER_KEY);

        assertThatThrownBy(() -> careRelationshipService.designateCaregiverAvatar(
                CareFixture.GUARDIAN_MEMBER_KEY, CareFixture.WARD_MEMBER_KEY,
                "stranger-key", CareFixture.ADULT_AVATAR_CODE))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);

        then(designatedAvatarManager).should(times(0)).designate(any(), any(), any(), any());
    }
}
