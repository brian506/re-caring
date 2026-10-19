package com.recaring.auth.implement.oauth;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.vo.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuthReader {

    private final OAuthRepository oAuthRepository;

    public Optional<String> findMemberKey(OAuthProvider provider, String providerMemberId) {
        return oAuthRepository.find(provider, providerMemberId).map(OAuth::getMemberKey);
    }

    public boolean isLinked(String memberKey, OAuthProvider provider) {
        return oAuthRepository.existsOAuthLink(memberKey, provider);
    }

}
