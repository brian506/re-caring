package com.recaring.auth.vo;

public record OAuthUser(
        String providerMemberId,
        OAuthProvider provider,
        String name
) {
}
