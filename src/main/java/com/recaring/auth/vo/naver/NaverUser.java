package com.recaring.auth.vo.naver;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;

public record NaverUser(
        String resultcode,
        String message,
        NaverResponse response
) {
    public OAuthUser toOAuthUser() {
        return new OAuthUser(
                response.id(),
                OAuthProvider.NAVER,
                response.email(),
                response.name()
        );
    }
}
