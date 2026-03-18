package com.recaring.auth.vo;

public record OAuthUser(
        String providerUserId,
        OAuthProvider provider,
        String email,
        String name
) {
}
