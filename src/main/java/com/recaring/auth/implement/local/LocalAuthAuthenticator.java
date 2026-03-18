package com.recaring.auth.implement.local;

import com.recaring.auth.business.command.SignInCommand;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.domain.member.dataaccess.entity.Member;
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
    private final LocalAuthReader localAuthReader;
    private final MemberReader memberReader;

    public EncodedPassword encodePassword(Password password) {
        return new EncodedPassword(passwordEncoder.encode(password.value()));
    }

    public Member authenticate(SignInCommand command) {
        LocalAuth localAuth = localAuthReader.findByEmail(command.email().value());
        if (!passwordEncoder.matches(command.password().value(), localAuth.getPassword())) {
            throw new AppException(ErrorType.INVALID_PASSWORD);
        }
        return memberReader.findByMemberKey(localAuth.getMemberKey());
    }
}
