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
import com.recaring.care.implement.CareRelationshipWriter;
import com.recaring.device.dataaccess.repository.WardDeviceTokenRepository;
import com.recaring.location.dataaccess.repository.LocationSettingRepository;
import com.recaring.location.implement.detection.BatteryAlertStateManager;
import com.recaring.location.implement.gps.GpsHistoryManager;
import com.recaring.location.implement.safezone.SafeZoneStateManager;
import com.recaring.location.implement.gps.GpsLatestCacheManager;
import com.recaring.member.dataaccess.entity.Member;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberWithdrawalManager 단위 테스트")
class MemberWithdrawalManagerTest {

    @InjectMocks
    private MemberWithdrawalManager memberWithdrawalManager;

    @Mock
    private MemberReader memberReader;

    @Mock
    private MemberWriter memberWriter;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private LocalAuthAuthenticator localAuthAuthenticator;

    @Mock
    private MemberWithdrawalRepository memberWithdrawalRepository;

    @Mock
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private MembersTermsAgreementWriter membersTermsAgreementWriter;

    @Mock
    private CareRelationshipWriter careRelationshipWriter;

    @Mock
    private CareInvitationWriter careInvitationWriter;

    @Mock
    private GpsHistoryManager gpsHistoryManager;

    @Mock
    private SafeZoneWriter safeZoneWriter;

    @Mock
    private GpsLatestCacheManager gpsLatestCacheManager;

    @Mock
    private BatteryAlertStateManager batteryAlertStateManager;

    @Mock
    private SafeZoneStateManager safeZoneStateManager;

    @Mock
    private LocalAuthRepository localAuthRepository;

    @Mock
    private OAuthRepository oAuthRepository;

    @Mock
    private FcmDeviceTokenRepository fcmDeviceTokenRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private LocationSettingRepository locationSettingRepository;

    @Mock
    private WardDeviceTokenRepository wardDeviceTokenRepository;

    @Test
    @DisplayName("탈퇴 성공 - 비밀번호 검증 후 이력을 저장하고 연관 데이터를 모두 삭제한다")
    void withdraw_success() {
        // given
        String memberKey = AuthFixture.MEMBER_KEY;
        Password password = AuthFixture.createPassword();
        Member member = MemberFixture.createMember();
        LocalAuth localAuth = LocalAuth.builder()
                .memberKey(memberKey)
                .email(AuthFixture.EMAIL)
                .password(AuthFixture.ENCODED_PASSWORD)
                .build();

        given(memberReader.findByMemberKey(memberKey)).willReturn(member);
        given(localAuthReader.findByMemberKey(memberKey)).willReturn(localAuth);

        // when
        memberWithdrawalManager.withdraw(memberKey, password);

        // then
        then(localAuthAuthenticator).should(times(1)).verifyPassword(memberKey, password);
        then(memberWithdrawalRepository).should(times(1)).save(any());

        then(refreshTokenWriter).should(times(1)).deleteByMemberKey(memberKey);
        then(localAuthRepository).should(times(1)).deleteByMemberKey(memberKey);
        then(oAuthRepository).should(times(1)).deleteByMemberKey(memberKey);
        then(membersTermsAgreementWriter).should(times(1)).deleteByMemberKey(memberKey);
        then(fcmDeviceTokenRepository).should(times(1)).deleteByMemberKey(memberKey);
        then(notificationSettingRepository).should(times(1)).deleteByWardMemberKey(memberKey);
        then(careRelationshipWriter).should(times(1)).deleteAllByMemberKey(memberKey);
        then(careInvitationWriter).should(times(1)).deleteAllByMemberKey(memberKey);
        then(gpsHistoryManager).should(times(1)).deleteByWardMemberKey(memberKey);
        then(locationSettingRepository).should(times(1)).deleteByWardMemberKey(memberKey);
        then(safeZoneWriter).should(times(1)).deleteByWardMemberKey(memberKey);
        then(wardDeviceTokenRepository).should(times(1)).deleteByWardKey(memberKey);

        then(memberWriter).should(times(1)).deleteByMemberKey(memberKey);
        then(gpsLatestCacheManager).should(times(1)).delete(memberKey);
        then(batteryAlertStateManager).should(times(1)).delete(memberKey);
        then(safeZoneStateManager).should(times(1)).delete(memberKey);
    }

    @Test
    @DisplayName("탈퇴 실패 - 비밀번호가 일치하지 않으면 AppException 발생하고 어떤 데이터도 삭제하지 않는다")
    void withdraw_fail_with_wrong_password() {
        // given
        String memberKey = AuthFixture.MEMBER_KEY;
        Password password = AuthFixture.createPassword();

        willThrow(new AppException(ErrorType.INVALID_PASSWORD))
                .given(localAuthAuthenticator).verifyPassword(memberKey, password);

        // when & then
        assertThatThrownBy(() -> memberWithdrawalManager.withdraw(memberKey, password))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);

        then(memberWithdrawalRepository).should(never()).save(any());
        then(memberWriter).should(never()).deleteByMemberKey(memberKey);
        then(refreshTokenWriter).should(never()).deleteByMemberKey(memberKey);
    }
}
