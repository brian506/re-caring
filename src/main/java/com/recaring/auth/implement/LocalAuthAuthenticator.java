package com.recaring.auth.implement;

import com.recaring.auth.business.command.SignInCommand;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.domain.member.Member;
import com.recaring.domain.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalAuthAuthenticator {

    private final PasswordEncoder passwordEncoder;
    private final MemberReader memberReader;

    public EncodedPassword encodePassword(Password password) {
        return new EncodedPassword(passwordEncoder.encode(password.password()));
    }

    public Member authenticate(SignInCommand command) {
        Member member = memberReader.findByEmail(command.email().email());
        if (!passwordEncoder.matches(command.password().password(), member.getPassword())) {
            throw new AppException(ErrorType.INVALID_PASSWORD);
        }
        return member;
    }
}
