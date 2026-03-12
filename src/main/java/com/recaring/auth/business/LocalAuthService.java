package com.recaring.auth.business;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.implement.LocalAuthAuthenticator;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.domain.member.implement.MemberWriter;
import com.recaring.security.jwt.JwtGenerator;
import com.recaring.sms.implement.PhoneVerificationReader;
import com.recaring.sms.implement.PhoneVerificationWriter;
import com.recaring.sms.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final JwtGenerator jwtGenerator;
    private final LocalAuthAuthenticator authAuthenticator;
    private final MemberWriter memberWriter;
    private final PhoneVerificationReader phoneVerificationReader;
    private final PhoneVerificationWriter phoneVerificationWriter;

    public void signUp(SignUpCommand command) {
        PhoneNumber phone = phoneVerificationReader.findPhoneByToken(command.verificationToken());
        EncodedPassword encodedPassword = authAuthenticator.encodePassword(command.password());
        memberWriter.registerMember(command, encodedPassword, phone.value());
        phoneVerificationWriter.deleteToken(command.verificationToken());
    }
}
