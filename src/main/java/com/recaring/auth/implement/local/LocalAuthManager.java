package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MemberWriter;
import com.recaring.member.implement.MembersTermsAgreementWriter;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LocalAuthManager {

    private final LocalAuthReader localAuthReader;
    private final LocalAuthRepository localAuthRepository;
    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final MembersTermsAgreementWriter termsAgreementWriter;

    @Transactional
    public void register(NewLocalMember member) {
        if (memberReader.existsByPhone(member.phone())) {
            throw new AppException(ErrorType.ALREADY_REGISTERED_PHONE);
        }
        if (localAuthRepository.existsByEmail(member.email().value())) {
            throw new AppException(ErrorType.ALREADY_REGISTERED_EMAIL);
        }
        String memberKey = memberWriter.registerLocalMember(member);
        localAuthRepository.save(LocalAuth.of(memberKey, member.email().value(), member.password().value()));
        termsAgreementWriter.register(memberKey);
    }

    @Transactional
    public void changePassword(String memberKey, String encodedPassword) {
        LocalAuth localAuth = localAuthReader.findByMemberKey(memberKey);
        localAuth.updatePassword(encodedPassword);
    }
}
