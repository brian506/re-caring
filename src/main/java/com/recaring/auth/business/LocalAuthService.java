package com.recaring.auth.business;

import com.recaring.auth.business.command.SignInCommand;
import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.implement.local.LocalAuthAuthenticator;
import com.recaring.auth.implement.local.LocalAuthManager;
import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.implement.TokenIssuer;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.common.utils.MaskingUtils;
import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.security.vo.Jwt;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final TokenIssuer tokenIssuer;
    private final LocalAuthAuthenticator authAuthenticator;
    private final MemberReader memberReader;
    private final LocalAuthManager localAuthManager;
    private final LocalAuthReader localAuthReader;
    private final RefreshTokenWriter refreshTokenWriter;
    private final PhoneVerificationReader phoneVerificationReader;

    public void signUp(SignUpCommand command) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(command.smsToken());
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(command.password());
        localAuthManager.register(command.toNewLocalMember(phone, encodedPassword));
    }

    public Jwt signIn(SignInCommand command) {
        Member member = authAuthenticator.authenticate(command);
        return tokenIssuer.issue(member);
    }

    public String findEmail(String name, LocalDate birth, PhoneNumber phone) {
        Member member = memberReader.findByNameAndBirthAndPhone(name, birth, phone.value());
        return MaskingUtils.maskEmail(localAuthReader.findByMemberKey(member.getMemberKey()).getEmail());
    }

    public void findPassword(String smsToken, Password password) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(smsToken);
        Member member = memberReader.findByPhone(phone.value());
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(password);
        localAuthManager.changePassword(member.getMemberKey(), encodedPassword.value());
    }

    public void signOut(String refreshToken) {
        refreshTokenWriter.delete(refreshToken);
    }
}
