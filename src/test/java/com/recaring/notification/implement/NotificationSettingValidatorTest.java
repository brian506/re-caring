package com.recaring.notification.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.fixture.NotificationFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingValidator 단위 테스트")
class NotificationSettingValidatorTest {

    @InjectMocks
    private NotificationSettingValidator notificationSettingValidator;

    @Mock
    private MemberReader memberReader;
    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Test
    @DisplayName("대상자 본인은 알림 설정 접근 권한 검증을 통과한다")
    void validateSettingAccess_passes_for_ward_self() {
        given(memberReader.findByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.createWard());

        notificationSettingValidator.validateSettingAccess(
                NotificationFixture.WARD_KEY,
                NotificationFixture.WARD_KEY
        );

        then(careRelationshipRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("주보호자 케어 관계가 있으면 알림 설정 접근 권한 검증을 통과한다")
    void validateSettingAccess_passes_for_guardian_relationship() {
        given(memberReader.findByMemberKey(NotificationFixture.GUARDIAN_KEY))
                .willReturn(NotificationFixture.createGuardian());
        given(memberReader.findByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.createWard());
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                NotificationFixture.WARD_KEY,
                NotificationFixture.GUARDIAN_KEY,
                CareRole.GUARDIAN
        )).willReturn(true);

        notificationSettingValidator.validateSettingAccess(
                NotificationFixture.GUARDIAN_KEY,
                NotificationFixture.WARD_KEY
        );

        then(careRelationshipRepository).should().existsByWardKeyAndCaregiverKeyAndCareRole(
                NotificationFixture.WARD_KEY,
                NotificationFixture.GUARDIAN_KEY,
                CareRole.GUARDIAN
        );
    }

    @Test
    @DisplayName("관계자 케어 관계가 있으면 알림 설정 접근 권한 검증을 통과한다")
    void validateSettingAccess_passes_for_manager_relationship() {
        given(memberReader.findByMemberKey(NotificationFixture.MANAGER_KEY))
                .willReturn(NotificationFixture.createManager());
        given(memberReader.findByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.createWard());
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                NotificationFixture.WARD_KEY,
                NotificationFixture.MANAGER_KEY,
                CareRole.GUARDIAN
        )).willReturn(false);
        given(careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                NotificationFixture.WARD_KEY,
                NotificationFixture.MANAGER_KEY,
                CareRole.MANAGER
        )).willReturn(true);

        notificationSettingValidator.validateSettingAccess(
                NotificationFixture.MANAGER_KEY,
                NotificationFixture.WARD_KEY
        );

        then(careRelationshipRepository).should().existsByWardKeyAndCaregiverKeyAndCareRole(
                NotificationFixture.WARD_KEY,
                NotificationFixture.MANAGER_KEY,
                CareRole.MANAGER
        );
    }

    @Test
    @DisplayName("케어 관계가 없으면 알림 설정 접근 권한 검증에 실패한다")
    void validateSettingAccess_throws_for_unrelated_member() {
        given(memberReader.findByMemberKey(NotificationFixture.OTHER_GUARDIAN_KEY))
                .willReturn(NotificationFixture.createOtherGuardian());
        given(memberReader.findByMemberKey(NotificationFixture.WARD_KEY))
                .willReturn(NotificationFixture.createWard());

        assertThatThrownBy(() -> notificationSettingValidator.validateSettingAccess(
                NotificationFixture.OTHER_GUARDIAN_KEY,
                NotificationFixture.WARD_KEY
        ))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_CARE_RELATED_WARD);
    }
}
