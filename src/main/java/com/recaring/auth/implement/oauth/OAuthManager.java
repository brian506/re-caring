package com.recaring.auth.implement.oauth;

import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.vo.NewOauthMember;
import com.recaring.common.mapper.auth.AuthMapper;
import com.recaring.member.implement.MemberWriter;
import com.recaring.member.implement.MemberWriter;
import com.recaring.member.implement.MembersTermsAgreementWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OAuthManager {

    private final OAuthRepository oAuthRepository;
    private final MemberWriter memberWriter;
    private final MembersTermsAgreementWriter termsAgreementWriter;
    private final AuthMapper authMapper;

    @Transactional
    public String register(NewOauthMember newOauthMember) {
        String memberKey = memberWriter.registerOAuthMember(newOauthMember);
        oAuthRepository.save(authMapper.createOAuth(memberKey, newOauthMember.provider(), newOauthMember.providerMemberId()));
        termsAgreementWriter.register(memberKey);
        return memberKey;
    }
}
