package com.recaring.location.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.location.fixture.LocationFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationValidator 단위 테스트")
class LocationValidatorTest {

    @InjectMocks
    private LocationValidator locationValidator;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private MemberReader memberReader;

    @Test
    @DisplayName("WARD 역할 회원은 GPS 전송 권한 검증을 통과한다")
    void validateWardRole_passes_for_ward() {
        given(memberReader.findByMemberKey(LocationFixture.WARD_KEY)).willReturn(LocationFixture.createWard());

        assertThatNoException().isThrownBy(() -> locationValidator.validateWardRole(LocationFixture.WARD_KEY));
    }

    @Test
    @DisplayName("GUARDIAN 역할 회원이 GPS 전송 시 예외가 발생한다")
    void validateWardRole_throws_for_guardian() {
        given(memberReader.findByMemberKey(LocationFixture.GUARDIAN_KEY)).willReturn(LocationFixture.createGuardian());

        assertThatThrownBy(() -> locationValidator.validateWardRole(LocationFixture.GUARDIAN_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_WARD_MEMBER);
    }

    @Test
    @DisplayName("GUARDIAN 케어 관계가 있으면 접근 권한 검증을 통과한다")
    void validateCaregiverAccess_passes_for_guardian_with_relationship() {
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                LocationFixture.WARD_KEY, LocationFixture.GUARDIAN_KEY, CareRole.GUARDIAN))
                .willReturn(true);

        assertThatNoException().isThrownBy(() ->
                locationValidator.validateCaregiverAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY));
    }

    @Test
    @DisplayName("케어 관계가 없으면 예외가 발생한다")
    void validateCaregiverAccess_throws_when_no_relationship() {
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(any(), any(), any()))
                .willReturn(false);

        assertThatThrownBy(() ->
                locationValidator.validateCaregiverAccess(LocationFixture.GUARDIAN_KEY, LocationFixture.WARD_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CARE_RELATED_WARD);
    }
}
