package com.recaring.auth.implement.local;

import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.Password;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.vo.MemberInfo;
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

    public MemberInfo authenticate(PhoneNumber phone, Password password) {
        Optional<MemberInfo> member = memberReader.findOptionalByPhone(phone);
        Optional<EncodedPassword> storedPassword = member.flatMap(m -> localAuthReader.findOptionalPasswordByMemberKey(m.memberKey()));
        String encodedPassword = storedPassword.map(EncodedPassword::value).orElse(dummyEncodedPassword);
        boolean matches = passwordEncoder.matches(password.value(), encodedPassword);
        if (storedPassword.isEmpty() || !matches) {
            throw new AppException(ErrorType.INVALID_CREDENTIALS);
        }
        return member.get();
    }

    public void verifyPassword(String memberKey, Password password) {
        EncodedPassword storedPassword = localAuthReader.findPasswordByMemberKey(memberKey);
        if (!passwordEncoder.matches(password.value(), storedPassword.value())) {
            throw new AppException(ErrorType.INVALID_PASSWORD);
        }
    }
}
