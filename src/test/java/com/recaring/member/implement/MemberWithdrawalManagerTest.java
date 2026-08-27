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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberWithdrawalManager 단위 테스트")
class MemberWithdrawalManagerTest {

    private static final String MEMBER_KEY = MemberFixture.MEMBER_KEY;
    private static final Password PASSWORD = new Password(MemberFixture.CURRENT_PASSWORD);

    @InjectMocks
    private MemberWithdrawalManager memberWithdrawalManager;

    @Mock private MemberReader memberReader;
    @Mock private MemberWriter memberWriter;
    @Mock private LocalAuthReader localAuthReader;
    @Mock private LocalAuthAuthenticator localAuthAuthenticator;
    @Mock private MemberWithdrawalRepository memberWithdrawalRepository;
    @Mock private RefreshTokenWriter refreshTokenWriter;
    @Mock private MembersTermsAgreementWriter membersTermsAgreementWriter;
    @Mock private CareRelationshipWriter careRelationshipWriter;
    @Mock private CareInvitationWriter careInvitationWriter;
    @Mock private GpsHistoryManager gpsHistoryManager;
    @Mock private SafeZoneWriter safeZoneWriter;
    @Mock private GpsLatestCacheManager gpsLatestCacheManager;
    @Mock private BatteryAlertStateManager batteryAlertStateManager;
    @Mock private SafeZoneStateManager safeZoneStateManager;
    @Mock private LocalAuthRepository localAuthRepository;
    @Mock private OAuthRepository oAuthRepository;
    @Mock private FcmDeviceTokenRepository fcmDeviceTokenRepository;
    @Mock private NotificationSettingRepository notificationSettingRepository;
    @Mock private LocationSettingRepository locationSettingRepository;
    @Mock private WardDeviceTokenRepository wardDeviceTokenRepository;

    private Member wardMember;

    @BeforeEach
    void setUp() {
        wardMember = MemberFixture.createWardMember(MemberFixture.PHONE);
    }

    private void givenAuthenticatedWard() {
        given(memberReader.findByMemberKey(MEMBER_KEY)).willReturn(wardMember);
        given(localAuthReader.findByMemberKey(MEMBER_KEY))
                .willReturn(LocalAuth.of(MEMBER_KEY, MemberFixture.EMAIL, AuthFixture.ENCODED_PASSWORD));
    }

    @Test
    @DisplayName("탈퇴 이력에는 탈퇴한 회원의 memberKey·이메일·역할이 기록된다")
    void withdraw_records_withdrawal_history_of_that_member() {
        givenAuthenticatedWard();

        memberWithdrawalManager.withdraw(MEMBER_KEY, PASSWORD);

        ArgumentCaptor<MemberWithdrawal> captor = ArgumentCaptor.forClass(MemberWithdrawal.class);
        then(memberWithdrawalRepository).should().save(captor.capture());
        MemberWithdrawal history = captor.getValue();
        // SPEC snapshot.md / MemberWithdrawal 엔티티: member_key, email, role, withdrawn_at NOT NULL
        assertThat(history.getMemberKey()).isEqualTo(MEMBER_KEY);
        assertThat(history.getEmail()).isEqualTo(MemberFixture.EMAIL);
        assertThat(history.getRole()).isEqualTo(MemberRole.WARD);
    }

    @Test
    @DisplayName("탈퇴하면 인증·약관·케어·위치·안심존·알림·기기 토큰까지 해당 회원의 연관 데이터를 모두 삭제한다")
    void withdraw_deletes_every_associated_record_of_that_member() {
        givenAuthenticatedWard();

        memberWithdrawalManager.withdraw(MEMBER_KEY, PASSWORD);

        // SPEC DELETE /api/v1/members/me: 본인 계정 및 연관된 모든 데이터를 삭제한다
        then(memberWriter).should().deleteByMemberKey(MEMBER_KEY);
        then(localAuthRepository).should().deleteByMemberKey(MEMBER_KEY);
        then(oAuthRepository).should().deleteByMemberKey(MEMBER_KEY);
        then(refreshTokenWriter).should().deleteByMemberKey(MEMBER_KEY);
        then(membersTermsAgreementWriter).should().deleteByMemberKey(MEMBER_KEY);
        then(careRelationshipWriter).should().deleteAllByMemberKey(MEMBER_KEY);
        then(careInvitationWriter).should().deleteAllByMemberKey(MEMBER_KEY);
        then(gpsHistoryManager).should().deleteByWardMemberKey(MEMBER_KEY);
        then(locationSettingRepository).should().deleteByWardMemberKey(MEMBER_KEY);
        then(safeZoneWriter).should().deleteByWardMemberKey(MEMBER_KEY);
        then(fcmDeviceTokenRepository).should().deleteByMemberKey(MEMBER_KEY);
        then(notificationSettingRepository).should().deleteByWardMemberKey(MEMBER_KEY);
        then(wardDeviceTokenRepository).should().deleteByWardKey(MEMBER_KEY);
        // SPEC snapshot.md Redis 키: gps:latest / device:battery(탈퇴 시 삭제) / 안심존 상태 테이블
        then(gpsLatestCacheManager).should().delete(MEMBER_KEY);
        then(batteryAlertStateManager).should().delete(MEMBER_KEY);
        then(safeZoneStateManager).should().delete(MEMBER_KEY);
    }

    @Test
    @DisplayName("비밀번호 확인은 이력 저장과 회원 삭제보다 먼저 수행된다")
    void withdraw_verifies_password_before_any_mutation() {
        givenAuthenticatedWard();

        memberWithdrawalManager.withdraw(MEMBER_KEY, PASSWORD);

        // SPEC DELETE /api/v1/members/me: 비밀번호 확인 후 삭제한다
        InOrder inOrder = inOrder(localAuthAuthenticator, memberWithdrawalRepository, memberWriter);
        inOrder.verify(localAuthAuthenticator).verifyPassword(MEMBER_KEY, PASSWORD);
        inOrder.verify(memberWithdrawalRepository).save(any(MemberWithdrawal.class));
        inOrder.verify(memberWriter).deleteByMemberKey(MEMBER_KEY);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_PASSWORD 예외가 발생하고 이력 저장도 삭제도 일어나지 않는다")
    void withdraw_throws_and_mutates_nothing_when_password_wrong() {
        Password wrongPassword = new Password(MemberFixture.WRONG_PASSWORD);
        willThrow(new AppException(ErrorType.INVALID_PASSWORD))
                .given(localAuthAuthenticator).verifyPassword(MEMBER_KEY, wrongPassword);

        // SPEC ErrorType.INVALID_PASSWORD(E2017): 비밀번호가 일치하지 않습니다
        assertThatThrownBy(() -> memberWithdrawalManager.withdraw(MEMBER_KEY, wrongPassword))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PASSWORD);

        // SPEC DELETE /api/v1/members/me: 확인 실패 시 어떤 데이터도 삭제하지 않는다
        then(memberWithdrawalRepository).should(never()).save(any());
        verifyNoInteractions(memberWriter, localAuthRepository, oAuthRepository, refreshTokenWriter,
                membersTermsAgreementWriter, careRelationshipWriter, careInvitationWriter,
                gpsHistoryManager, locationSettingRepository, safeZoneWriter, fcmDeviceTokenRepository,
                notificationSettingRepository, wardDeviceTokenRepository,
                gpsLatestCacheManager, batteryAlertStateManager, safeZoneStateManager);
    }

    @Test
    @DisplayName("로컬 인증 정보가 없는 회원의 탈퇴는 NOT_FOUND_ACCOUNT 예외로 끝나고 삭제가 일어나지 않는다")
    void withdraw_throws_when_local_auth_absent() {
        given(memberReader.findByMemberKey(MEMBER_KEY)).willReturn(wardMember);
        willThrow(new AppException(ErrorType.NOT_FOUND_ACCOUNT))
                .given(localAuthReader).findByMemberKey(MEMBER_KEY);

        // SPEC ErrorType.NOT_FOUND_ACCOUNT(E2016)
        assertThatThrownBy(() -> memberWithdrawalManager.withdraw(MEMBER_KEY, PASSWORD))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_ACCOUNT);

        then(memberWithdrawalRepository).should(never()).save(any());
        then(memberWriter).should(never()).deleteByMemberKey(MEMBER_KEY);
    }
}
