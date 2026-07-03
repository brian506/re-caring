package com.recaring.auth.implement.oauth;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.dataaccess.repository.OAuthRepository;
import com.recaring.auth.vo.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OAuthManager {

    private final OAuthRepository oAuthRepository;
    private final OAuthLinkValidator oAuthLinkValidator;

    @Transactional
    public void link(String memberKey, OAuthProvider provider, String providerMemberId) {
        oAuthLinkValidator.validateLinkable(memberKey, provider);
        oAuthRepository.save(OAuth.of(memberKey, provider, providerMemberId));
    }
}
