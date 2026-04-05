package com.recaring.auth.implement.local;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.auth.dataaccess.repository.LocalAuthRepository;
import com.recaring.common.mapper.auth.AuthMapper;
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
    private final MemberWriter memberWriter;
    private final MembersTermsAgreementWriter termsAgreementWriter;
    private final AuthMapper mapper;

    @Transactional
    public void register(NewLocalMember member) {
        if(localAuthRepository.existsByEmail(member.email().value())) {
            throw new AppException(ErrorType.INVALID_EMAIL);
        }
        String memberKey = memberWriter.registerLocalMember(member);
        localAuthRepository.save(mapper.createLocalAuth(memberKey, member.email().value(), member.password().value()));
        termsAgreementWriter.register(memberKey);
    }

    @Transactional
    public void changePassword(String memberKey, String encodedPassword) {
        LocalAuth localAuth = localAuthReader.findByMemberKey(memberKey);
        localAuth.updatePassword(encodedPassword);
    }
}
