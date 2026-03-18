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

    public Optional<OAuth> findOAuthUser(OAuthProvider provider, String providerUserId) {
        return oAuthRepository.findOAuthUser(provider, providerUserId);
    }

}
