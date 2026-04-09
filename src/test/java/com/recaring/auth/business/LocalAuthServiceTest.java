package com.recaring.auth.business;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.fixture.MemberFixture;
import com.recaring.member.implement.MemberReader;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.vo.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService 단위 테스트")
class LocalAuthServiceTest {

    @InjectMocks
    private LocalAuthService localAuthService;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private LocalAuthAuthenticator authAuthenticator;

    @Mock
    private MemberReader memberReader;

    @Mock
    private LocalAuthManager localAuthManager;

    @Mock
    private LocalAuthReader localAuthReader;

    @Mock
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private PhoneVerificationReader phoneVerificationReader;

    @Test
    @DisplayName("회원가입 시 전화번호 인증 후 멤버가 등록된다")
    void signUp_success() {
        // given
        String verificationToken = UUID.randomUUID().toString();
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        EncodedPassword encodedPassword = AuthFixture.createEncodedPassword();
        SignUpCommand command = AuthFixture.createSignUpCommand(verificationToken);

        given(phoneVerificationReader.findPhoneByToken(verificationToken)).willReturn(phone);
        given(authAuthenticator.encodePassword(command.password())).willReturn(encodedPassword);

        // when
        localAuthService.signUp(command);

        //then
        then(localAuthManager).should(times(1)).register(any());
    }

    @Test
    @DisplayName("로그인 시 TokenIssuer를 통해 JWT가 발급된다")
    void signIn_success() {
        // given
        Member member = MemberFixture.createMember();
        LocalEmail email = AuthFixture.createLocalEmail();
        Password password = AuthFixture.createPassword();
        Jwt expectedJwt = AuthFixture.createJwt();

        given(authAuthenticator.authenticate(email, password)).willReturn(member);
        given(tokenIssuer.issue(member)).willReturn(expectedJwt);

        // when
        Jwt result = localAuthService.signIn(email, password);

        // then
        assertThat(result.accessToken()).isEqualTo(AuthFixture.ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(AuthFixture.REFRESH_TOKEN);
    }

    @Test
    @DisplayName("이메일 찾기 시 마스킹된 이메일이 반환된다")
    void findEmail_success() {
        // given
        Member member = MemberFixture.createMember();
        PhoneNumber phone = SmsFixture.createPhoneNumber();

        LocalAuth localAuth = LocalAuth.builder()
                .memberKey(member.getMemberKey())
                .email("hongildong@example.com")
                .password(AuthFixture.ENCODED_PASSWORD)
                .build();

        given(memberReader.findEmail(
                MemberFixture.NAME, MemberFixture.BIRTH, SmsFixture.PHONE))
                .willReturn(member);
        given(localAuthReader.findByMemberKey(member.getMemberKey())).willReturn(localAuth);

        // when
        String maskedEmail = localAuthService.findEmail(MemberFixture.NAME, MemberFixture.BIRTH, phone);

        //then
        assertThat(maskedEmail).isEqualTo("hon****@example.com");
    }

    @Test
    @DisplayName("비밀번호 재설정 시 전화번호 인증 후 비밀번호가 변경된다")
    void findPassword_success() {
        //given
        String smsToken = UUID.randomUUID().toString();
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        Password newPassword = new Password("newPass12");
        EncodedPassword encodedPassword = new EncodedPassword("$2a$10$newEncoded");
        Member member = MemberFixture.createMember();

        given(phoneVerificationReader.findPhoneByToken(smsToken)).willReturn(phone);
        given(memberReader.findByPhone(new PhoneNumber(SmsFixture.PHONE))).willReturn(member);
        given(authAuthenticator.encodePassword(newPassword)).willReturn(encodedPassword);

        // when
        localAuthService.findPassword(smsToken, newPassword);

        // then
        then(localAuthManager).should(times(1))
                .changePassword(member.getMemberKey(), encodedPassword.value());
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰이 삭제된다")
    void signOut_success() {
        String refreshToken = "some-refresh-token";

        localAuthService.signOut(refreshToken);

        then(refreshTokenWriter).should(times(1)).delete(refreshToken);
    }
}
