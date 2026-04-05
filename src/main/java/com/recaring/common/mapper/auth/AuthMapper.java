package com.recaring.common.mapper.auth;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.vo.OAuthProvider;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public LocalAuth createLocalAuth(String memberKey, String email, String password) {
        return LocalAuth.builder()
                .memberKey(memberKey)
                .email(email)
                .password(password)
                .build();
    }

    public OAuth createOAuth(String memberKey, OAuthProvider provider, String providerMemberId) {
        return OAuth.builder()
                .memberKey(memberKey)
                .provider(provider)
                .providerMemberId(providerMemberId)
                .build();
    }
}
