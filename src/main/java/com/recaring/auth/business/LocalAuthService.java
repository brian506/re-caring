package com.recaring.auth.business;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.notification.business.FcmDeviceTokenService;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final TokenIssuer tokenIssuer;
    private final LocalAuthAuthenticator authAuthenticator;
    private final MemberReader memberReader;
    private final LocalAuthManager localAuthManager;
    private final RefreshTokenWriter refreshTokenWriter;
    private final PhoneVerificationWriter phoneVerificationWriter;
    private final FcmDeviceTokenService fcmDeviceTokenService;

    public void signUp(SignUpCommand command) {
        PhoneNumber phone = phoneVerificationWriter.consumePhoneByToken(command.smsToken());
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(command.password());
        localAuthManager.register(command.toNewLocalMember(phone, encodedPassword));
    }

    public Jwt signIn(PhoneNumber phone, Password password) {
        Member member = authAuthenticator.authenticate(phone, password);
        return tokenIssuer.issue(member);
    }

    public void resetPassword(String smsToken, Password password) {
        PhoneNumber phone = phoneVerificationWriter.consumePhoneByToken(smsToken);
        Member member = memberReader.findByPhone(phone);
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(password);
        localAuthManager.changePassword(member.getMemberKey(), encodedPassword.value());
    }

    public void signOut(String refreshToken, String fcmToken) {
        refreshTokenWriter.delete(refreshToken);
        if (fcmToken != null && !fcmToken.isBlank()) {
            fcmDeviceTokenService.delete(fcmToken);
        }
    }
}
