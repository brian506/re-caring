package com.recaring.auth.business;

import com.recaring.auth.business.command.SignInCommand;
import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.implement.LocalAuthAuthenticator;
import com.recaring.auth.implement.RefreshTokenReader;
import com.recaring.auth.implement.RefreshTokenWriter;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.domain.member.Member;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.domain.member.implement.MemberWriter;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.security.jwt.JwtValidator;
import com.recaring.security.vo.Jwt;
import com.recaring.security.vo.TokenPayload;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final JwtGenerator jwtGenerator;
    private final JwtValidator jwtValidator;
    private final LocalAuthAuthenticator authAuthenticator;
    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final RefreshTokenReader refreshTokenReader;
    private final RefreshTokenWriter refreshTokenWriter;
    private final PhoneVerificationReader phoneVerificationReader;
    private final PhoneVerificationWriter phoneVerificationWriter;

    public void signUp(SignUpCommand command) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(command.verificationToken());
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(command.password());
        memberWriter.registerMember(command, encodedPassword, phone.value());
        phoneVerificationWriter.deleteToken(command.verificationToken());
    }

    public Jwt signIn(SignInCommand command) {
        Member member = authAuthenticator.authenticate(command);
        Jwt jwt = jwtGenerator.generateJwt(new TokenPayload(member.getMemberKey(), member.getRole(), new Date()));
        refreshTokenWriter.save(jwt.refreshToken(), member.getMemberKey());
        return jwt;
    }

    public void signOut(String refreshToken) {
        refreshTokenWriter.delete(refreshToken);
    }
}
