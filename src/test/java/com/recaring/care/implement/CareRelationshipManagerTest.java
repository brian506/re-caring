package com.recaring.care.implement;

import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.fixture.CareFixture;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import com.recaring.location.implement.setting.LocationSettingManager;
import com.recaring.notification.implement.setting.NotificationSettingManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareRelationshipManager 단위 테스트")
class CareRelationshipManagerTest {

    private static final String WARD_KEY = CareFixture.WARD_MEMBER_KEY;
    private static final String LEAVING_GUARDIAN_KEY = "leaving-guardian-key";

    @InjectMocks
    private CareRelationshipManager careRelationshipManager;

    @Mock
    private CareRelationshipRepository careRelationshipRepository;

    @Mock
    private CareInvitationWriter careInvitationWriter;

    @Mock
    private DesignatedAvatarManager designatedAvatarManager;

    @Mock
    private NotificationSettingManager notificationSettingManager;

    @Mock
    private LocationSettingManager locationSettingManager;

    @Test
    @DisplayName("이탈하면 그 사람이 그 대상자에게 보낸 대기 중 초대가 정리된다")
    void leaveCare_clears_pending_invitations_sent_by_the_leaver() {
        given(careRelationshipRepository.findCareRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY))
                .willReturn(Optional.of(CareFixture.createGuardianRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY)));

        careRelationshipManager.leaveCare(WARD_KEY, LEAVING_GUARDIAN_KEY);

        then(careInvitationWriter).should().deletePendingSentBy(WARD_KEY, LEAVING_GUARDIAN_KEY);
    }

    @Test
    @DisplayName("케어 관계가 없는 사람이 이탈하려 하면 예외가 발생하고 아무것도 지우지 않는다")
    void leaveCare_fails_when_relationship_not_found() {
        given(careRelationshipRepository.findCareRelationship(WARD_KEY, "stranger-key"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> careRelationshipManager.leaveCare(WARD_KEY, "stranger-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
        then(careInvitationWriter).should(never()).deletePendingSentBy(WARD_KEY, "stranger-key");
    }

    @Test
    @DisplayName("이탈하면 그 대상자 범위에서 그 사람이 지정했거나 지정받은 얼굴을 함께 지운다")
    void leaveCare_removes_designated_avatars_of_the_leaving_member() {
        given(careRelationshipRepository.findCareRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY))
                .willReturn(Optional.of(CareFixture.createGuardianRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY)));

        careRelationshipManager.leaveCare(WARD_KEY, LEAVING_GUARDIAN_KEY);

        then(designatedAvatarManager).should().deleteAllByRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY);
    }

    @Test
    @DisplayName("이탈하면 그 사람이 그 대상자에 대해 둔 알림 설정과 위치 수신 주기를 지운다")
    void leaveCare_removes_settings_of_the_leaving_caregiver() {
        given(careRelationshipRepository.findCareRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY))
                .willReturn(Optional.of(CareFixture.createGuardianRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY)));

        careRelationshipManager.leaveCare(WARD_KEY, LEAVING_GUARDIAN_KEY);

        then(notificationSettingManager).should().deleteByRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY);
        then(locationSettingManager).should().deleteByRelationship(WARD_KEY, LEAVING_GUARDIAN_KEY);
    }

    @Test
    @DisplayName("탈퇴하면 그 회원이 지정했거나 지정받은 얼굴을 모두 지운다")
    void leaveAllCare_removes_every_designated_avatar_of_the_withdrawing_member() {
        careRelationshipManager.leaveAllCare(LEAVING_GUARDIAN_KEY);

        then(designatedAvatarManager).should().deleteAllByMemberKey(LEAVING_GUARDIAN_KEY);
    }
}
