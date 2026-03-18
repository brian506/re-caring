package com.recaring.auth.controller.response;

import com.recaring.security.vo.Jwt;

public record OAuthSignInResponse(String status, String accessToken, String refreshToken, String providerUserId) {

    public static final String SUCCESS = "SUCCESS";
    public static final String NEED_SIGN_UP = "NEED_SIGN_UP";

    public static OAuthSignInResponse success(Jwt jwt) {
        return new OAuthSignInResponse(SUCCESS, jwt.accessToken(), jwt.refreshToken(), null);
    }

    public static OAuthSignInResponse needSignUp(String providerUserId) {
        return new OAuthSignInResponse(NEED_SIGN_UP, null, null, providerUserId);
    }
}
