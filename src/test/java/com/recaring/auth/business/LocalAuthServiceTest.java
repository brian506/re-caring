package com.recaring.auth.business;

import com.recaring.auth.fixture.AuthFixture;
import com.recaring.auth.business.command.SignInCommand;
import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.implement.LocalAuthAuthenticator;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.domain.member.Member;
import com.recaring.domain.member.fixture.MemberFixture;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.domain.member.implement.MemberWriter;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.vo.Jwt;
import com.recaring.security.vo.TokenPayload;
import com.recaring.sms.fixture.SmsFixture;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService 단위 테스트")
class LocalAuthServiceTest {

    @InjectMocks
    private LocalAuthService localAuthService;

    @Mock
    private JwtGenerator jwtGenerator;

    @Mock
    private LocalAuthAuthenticator authAuthenticator;

    @Mock
    private MemberWriter memberWriter;

    @Mock
    private MemberReader memberReader;

    @Mock
    private RefreshTokenWriter refreshTokenWriter;

    @Mock
    private PhoneVerificationReader phoneVerificationReader;

    @Mock
    private PhoneVerificationWriter phoneVerificationWriter;

    @Test
    @DisplayName("회원가입 시 전화번호 인증 후 멤버가 등록되고 토큰이 삭제된다")
    void signUp_success() {
        String verificationToken = UUID.randomUUID().toString();
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        EncodedPassword encodedPassword = AuthFixture.createEncodedPassword();
        SignUpCommand command = AuthFixture.createSignUpCommand(verificationToken);

        given(phoneVerificationReader.findPhoneByToken(verificationToken)).willReturn(phone);
        given(authAuthenticator.encodePassword(command.password())).willReturn(encodedPassword);

        localAuthService.signUp(command);

        then(memberWriter).should(times(1)).registerMember(eq(command), eq(encodedPassword), eq(SmsFixture.PHONE));
        then(phoneVerificationWriter).should(times(1)).deleteToken(verificationToken);
    }

    @Test
    @DisplayName("로그인 시 JWT가 생성되고 리프레시 토큰이 저장된다")
    void signIn_success() {
        Member member = MemberFixture.createMember();
        SignInCommand command = AuthFixture.createSignInCommand();
        Jwt expectedJwt = AuthFixture.createJwt();

        given(authAuthenticator.authenticate(command)).willReturn(member);
        given(jwtGenerator.generateJwt(any(TokenPayload.class))).willReturn(expectedJwt);

        Jwt result = localAuthService.signIn(command);

        assertThat(result.accessToken()).isEqualTo(AuthFixture.ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(AuthFixture.REFRESH_TOKEN);
        then(refreshTokenWriter).should(times(1)).save(AuthFixture.REFRESH_TOKEN, member.getMemberKey());
    }

    @Test
    @DisplayName("이메일 찾기 시 마스킹된 이메일이 반환된다")
    void findEmail_success() {
        Member member = MemberFixture.createMember("hongildong@example.com");
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        given(memberReader.findByNameAndBirthAndPhone(
                MemberFixture.NAME, MemberFixture.BIRTH, SmsFixture.PHONE))
                .willReturn(member);

        String maskedEmail = localAuthService.findEmail(MemberFixture.NAME, MemberFixture.BIRTH, phone);

        assertThat(maskedEmail).isEqualTo("hon****@example.com");
    }

    @Test
    @DisplayName("비밀번호 재설정 시 전화번호 인증 후 비밀번호가 변경되고 토큰이 삭제된다")
    void findPassword_success() {
        String smsToken = UUID.randomUUID().toString();
        PhoneNumber phone = SmsFixture.createPhoneNumber();
        Password newPassword = new Password("newPass12");
        EncodedPassword encodedPassword = new EncodedPassword("$2a$10$newEncoded");

        given(phoneVerificationReader.findPhoneByToken(smsToken)).willReturn(phone);
        given(authAuthenticator.encodePassword(newPassword)).willReturn(encodedPassword);

        localAuthService.findPassword(smsToken, newPassword);

        then(memberWriter).should(times(1)).changePassword(SmsFixture.PHONE, encodedPassword);
        then(phoneVerificationWriter).should(times(1)).deleteToken(smsToken);
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰이 삭제된다")
    void signOut_success() {
        String refreshToken = "some-refresh-token";

        localAuthService.signOut(refreshToken);

        then(refreshTokenWriter).should(times(1)).delete(refreshToken);
    }
}
