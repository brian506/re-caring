package com.recaring.auth.business;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.implement.LocalAuthAuthenticator;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.domain.member.implement.MemberWriter;
import com.recaring.security.jwt.JwtGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final JwtGenerator jwtGenerator;
    private final LocalAuthAuthenticator authAuthenticator;
    private final MemberWriter memberWriter;

    public void signUp(SignUpCommand command) {

        EncodedPassword encodedPassword = authAuthenticator.encodePassword(command.password());
        memberWriter.registerMember(command,encodedPassword);
    }
}
