package com.recaring.auth.vo.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.vo.OAuthUser;

public record KakaoUser(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public OAuthUser toOAuthUser() {
        return new OAuthUser(
                String.valueOf(id),
                OAuthProvider.KAKAO,
                kakaoAccount != null ? kakaoAccount.email() : null,
                kakaoAccount != null && kakaoAccount.profile() != null
                        ? kakaoAccount.profile().nickname()
                        : null
        );
    }
}
