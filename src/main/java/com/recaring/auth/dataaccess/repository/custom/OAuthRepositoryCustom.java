package com.recaring.auth.dataaccess.repository.custom;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.vo.OAuthProvider;

import java.util.Optional;

public interface OAuthRepositoryCustom {
    Optional<OAuth> findOAuthMember(OAuthProvider provider, String providerMemberId);
}
