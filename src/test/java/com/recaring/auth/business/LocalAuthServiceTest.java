package com.recaring.auth.business;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.auth.vo.Password;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.business.FcmDeviceTokenService;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.vo.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService 단위 테스트")
class LocalAuthServiceTest {

    @InjectMocks
    private LocalAuthService localAuthService;

    @Mock
    private LocalAuthAuthenticator authAuthenticator;

    @Mock
    private MemberReader memberReader;

    @Mock
    private LocalAuthManager localAuthManager;

    @Mock
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private PhoneVerificationWriter phoneVerificationWriter;

    @Mock
    private FcmDeviceTokenService fcmDeviceTokenService;

    @Test
    @DisplayName("회원가입 시 인증된 전화번호와 인코딩된 비밀번호로 멤버가 등록된다")
    void signUp_success() {
        // given
        String verificationToken = UUID.randomUUID().toString();
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        EncodedPassword encodedPassword = AuthFixture.createEncodedPassword();
        SignUpCommand command = AuthFixture.createSignUpCommand(verificationToken);

        given(phoneVerificationWriter.consumePhoneByToken(verificationToken)).willReturn(phone);
        given(authAuthenticator.encodePassword(command.password())).willReturn(encodedPassword);

        // when
        localAuthService.signUp(command);

        // then
        ArgumentCaptor<NewLocalMember> captor = ArgumentCaptor.forClass(NewLocalMember.class);
        then(localAuthManager).should(times(1)).register(captor.capture());
        NewLocalMember registered = captor.getValue();
        assertThat(registered.phone()).isEqualTo(phone);
        assertThat(registered.password()).isEqualTo(encodedPassword);
    }

    @Test
    @DisplayName("비밀번호 재설정 시 전화번호 인증 후 비밀번호가 변경된다")
    void resetPassword_success() {
        //given
        String smsToken = UUID.randomUUID().toString();
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        Password newPassword = new Password("newPass12");
        EncodedPassword encodedPassword = new EncodedPassword("$2a$10$newEncoded");
        Member member = MemberFixture.createMember();

        given(phoneVerificationWriter.consumePhoneByToken(smsToken)).willReturn(phone);
        given(memberReader.findByPhone(new PhoneNumber(SmsFixture.PHONE))).willReturn(member);
        given(authAuthenticator.encodePassword(newPassword)).willReturn(encodedPassword);

        // when
        localAuthService.resetPassword(smsToken, newPassword);

        // then
        then(localAuthManager).should(times(1))
                .changePassword(member.getMemberKey(), encodedPassword.value());
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰이 삭제된다")
    void signOut_success() {
        String refreshToken = "some-refresh-token";

        localAuthService.signOut(refreshToken, null);

        then(refreshTokenWriter).should(times(1)).delete(refreshToken);
        then(fcmDeviceTokenService).should(never()).delete(any());
    }

    @Test
    @DisplayName("로그아웃 시 FCM 토큰을 함께 보내면 해당 토큰도 삭제된다")
    void signOut_deletes_fcmToken_when_present() {
        String refreshToken = "some-refresh-token";
        String fcmToken = "some-fcm-token";

        localAuthService.signOut(refreshToken, fcmToken);

        then(refreshTokenWriter).should(times(1)).delete(refreshToken);
        then(fcmDeviceTokenService).should(times(1)).delete(fcmToken);
    }

    @Test
    @DisplayName("로그아웃 시 FCM 토큰이 공백이면 삭제를 시도하지 않는다")
    void signOut_does_not_delete_fcmToken_when_blank() {
        String refreshToken = "some-refresh-token";

        localAuthService.signOut(refreshToken, "  ");

        then(refreshTokenWriter).should(times(1)).delete(refreshToken);
        then(fcmDeviceTokenService).should(never()).delete(any());
    }
}
