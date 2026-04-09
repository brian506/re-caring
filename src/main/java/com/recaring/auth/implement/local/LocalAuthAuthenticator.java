package com.recaring.auth.implement.local;

import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
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

    public Member authenticate(LocalEmail email, Password password) {
        LocalAuth localAuth = localAuthReader.findByEmail(email.value());
        if (!passwordEncoder.matches(password.value(), localAuth.getPassword())) {
            throw new AppException(ErrorType.INVALID_PASSWORD);
        }
        return memberReader.findByMemberKey(localAuth.getMemberKey());
    }
}
