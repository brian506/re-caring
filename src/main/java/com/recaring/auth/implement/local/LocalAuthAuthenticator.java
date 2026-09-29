package com.recaring.auth.implement.local;

import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class LocalAuthAuthenticator {

    private final PasswordEncoder passwordEncoder;
    private final LocalAuthReader localAuthReader;
    private final MemberReader memberReader;
    private final String dummyEncodedPassword;

    public LocalAuthAuthenticator(PasswordEncoder passwordEncoder, LocalAuthReader localAuthReader,
                                  MemberReader memberReader) {
        this.passwordEncoder = passwordEncoder;
        this.localAuthReader = localAuthReader;
        this.memberReader = memberReader;
        this.dummyEncodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public EncodedPassword encodePassword(Password password) {
        return new EncodedPassword(passwordEncoder.encode(password.value()));
    }

    public Member authenticate(PhoneNumber phone, Password password) {
        Optional<Member> member = memberReader.findOptionalByPhone(phone);
        Optional<LocalAuth> localAuth = member.flatMap(m -> localAuthReader.findOptionalByMemberKey(m.getMemberKey()));
        String encodedPassword = localAuth.map(LocalAuth::getPassword).orElse(dummyEncodedPassword);
        boolean matches = passwordEncoder.matches(password.value(), encodedPassword);
        if (localAuth.isEmpty() || !matches) {
            throw new AppException(ErrorType.INVALID_CREDENTIALS);
        }
        return member.get();
    }

    public void verifyPassword(String memberKey, Password password) {
        LocalAuth localAuth = localAuthReader.findByMemberKey(memberKey);
        if (!passwordEncoder.matches(password.value(), localAuth.getPassword())) {
            throw new AppException(ErrorType.INVALID_PASSWORD);
        }
    }
}
