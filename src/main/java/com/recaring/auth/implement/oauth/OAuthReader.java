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

    public Optional<OAuth> find(OAuthProvider provider, String providerMemberId) {
        return oAuthRepository.find(provider, providerMemberId);
    }

    public boolean isLinked(String memberKey, OAuthProvider provider) {
        return oAuthRepository.existsOAuthLink(memberKey, provider);
    }

}
