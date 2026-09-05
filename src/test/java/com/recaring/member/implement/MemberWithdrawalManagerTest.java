package com.recaring.member.implement;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.auth.vo.Password;
import com.recaring.care.implement.CareInvitationWriter;
import com.recaring.care.implement.CareRelationshipManager;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.implement.detection.AnomalyDetectionManager;
import com.recaring.location.implement.detection.BatteryAlertStateManager;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import com.recaring.location.implement.safezone.SafeZoneStateManager;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.MemberWithdrawal;
import com.recaring.member.dataaccess.repository.MemberWithdrawalRepository;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.notification.dataaccess.repository.FcmDeviceTokenRepository;
import com.recaring.notification.dataaccess.repository.NotificationSettingRepository;
import com.recaring.safezone.implement.SafeZoneWriter;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberWithdrawalManager 단위 테스트")
class MemberWithdrawalManagerTest {

    private static final String MEMBER_KEY = AuthFixture.MEMBER_KEY;

    @InjectMocks
    private MemberWithdrawalManager memberWithdrawalManager;

    @Mock private MemberReader memberReader;
    @Mock private MemberWriter memberWriter;
    @Mock private LocalAuthReader localAuthReader;
    @Mock private LocalAuthAuthenticator localAuthAuthenticator;
    @Mock private MemberWithdrawalRepository memberWithdrawalRepository;

    @Mock private RefreshTokenWriter refreshTokenWriter;
    @Mock private MembersTermsAgreementWriter membersTermsAgreementWriter;
    @Mock private CareRelationshipManager careRelationshipManager;
    @Mock private CareInvitationWriter careInvitationWriter;
    @Mock private GpsHistoryManager gpsHistoryManager;
    @Mock private SafeZoneWriter safeZoneWriter;
    @Mock private GpsLatestCacheManager gpsLatestCacheManager;
    @Mock private AnomalyDetectionManager anomalyDetectionManager;
    @Mock private BatteryAlertStateManager batteryAlertStateManager;
    @Mock private SafeZoneStateManager safeZoneStateManager;

    @Mock private LocalAuthRepository localAuthRepository;
    @Mock private OAuthRepository oAuthRepository;
    @Mock private FcmDeviceTokenRepository fcmDeviceTokenRepository;
    @Mock private NotificationSettingRepository notificationSettingRepository;
    @Mock private LocationSettingRepository locationSettingRepository;
    @Mock private WardDeviceTokenRepository wardDeviceTokenRepository;

    @Captor
    private ArgumentCaptor<MemberWithdrawal> withdrawalCaptor;

    @Test
    @DisplayName("탈퇴하면 회원이 남긴 데이터가 도메인마다 하나도 빠짐없이 삭제된다")
    void withdraw_deletes_every_store_that_holds_member_data() {
        // Given
        givenWithdrawableMember();

        // When
        memberWithdrawalManager.withdraw(MEMBER_KEY, AuthFixture.createPassword());

        // Then
        then(refreshTokenWriter).should().deleteByMemberKey(MEMBER_KEY);
        then(localAuthRepository).should().deleteByMemberKey(MEMBER_KEY);
        then(oAuthRepository).should().deleteByMemberKey(MEMBER_KEY);
        then(membersTermsAgreementWriter).should().deleteByMemberKey(MEMBER_KEY);
        then(fcmDeviceTokenRepository).should().deleteByMemberKey(MEMBER_KEY);
        then(notificationSettingRepository).should().deleteByWardMemberKey(MEMBER_KEY);
        then(careRelationshipManager).should().leaveAllCare(MEMBER_KEY);
        then(careInvitationWriter).should().deleteAllByMemberKey(MEMBER_KEY);
        then(gpsHistoryManager).should().deleteByWardMemberKey(MEMBER_KEY);
        then(anomalyDetectionManager).should().deleteByWardMemberKey(MEMBER_KEY);
        then(locationSettingRepository).should().deleteByWardMemberKey(MEMBER_KEY);
        then(safeZoneWriter).should().deleteByWardMemberKey(MEMBER_KEY);
        then(wardDeviceTokenRepository).should().deleteByWardKey(MEMBER_KEY);
        then(memberWriter).should().deleteByMemberKey(MEMBER_KEY);

        then(gpsLatestCacheManager).should().delete(MEMBER_KEY);
        then(batteryAlertStateManager).should().delete(MEMBER_KEY);
        then(safeZoneStateManager).should().delete(MEMBER_KEY);
    }

    @Test
    @DisplayName("탈퇴 이력에는 삭제되기 전 회원의 memberKey·이메일·역할이 그대로 남는다")
    void withdraw_records_identity_of_the_deleted_member() {
        // Given
        givenWithdrawableMember();

        // When
        memberWithdrawalManager.withdraw(MEMBER_KEY, AuthFixture.createPassword());

        // Then
        then(memberWithdrawalRepository).should().save(withdrawalCaptor.capture());
        MemberWithdrawal withdrawal = withdrawalCaptor.getValue();
        assertThat(withdrawal.getMemberKey()).isEqualTo(MEMBER_KEY);
        assertThat(withdrawal.getEmail()).isEqualTo(AuthFixture.EMAIL);
        assertThat(withdrawal.getRole()).isEqualTo(MemberRole.WARD);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_PASSWORD 예외가 발생하고 어떤 데이터도 삭제되지 않는다")
    void withdraw_deletes_nothing_when_password_is_wrong() {
        // Given
        Password password = AuthFixture.createPassword();
        willThrow(new AppException(ErrorType.INVALID_PASSWORD))
                .given(localAuthAuthenticator).verifyPassword(MEMBER_KEY, password);

        // When / Then
        assertThatThrownBy(() -> memberWithdrawalManager.withdraw(MEMBER_KEY, password))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);

        then(memberWithdrawalRepository).should(never()).save(any());
        then(memberWriter).should(never()).deleteByMemberKey(MEMBER_KEY);
        then(refreshTokenWriter).should(never()).deleteByMemberKey(MEMBER_KEY);
        then(localAuthRepository).should(never()).deleteByMemberKey(MEMBER_KEY);
    }

    private void givenWithdrawableMember() {
        Member member = MemberFixture.createWardMember(MemberFixture.PHONE);
        LocalAuth localAuth = LocalAuth.of(MEMBER_KEY, AuthFixture.EMAIL, AuthFixture.ENCODED_PASSWORD);
        given(memberReader.findByMemberKey(MEMBER_KEY)).willReturn(member);
        given(localAuthReader.findByMemberKey(MEMBER_KEY)).willReturn(localAuth);
    }
}
