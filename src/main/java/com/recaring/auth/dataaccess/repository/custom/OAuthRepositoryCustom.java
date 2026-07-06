package com.recaring.auth.dataaccess.repository.custom;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.vo.OAuthProvider;

import java.util.Optional;

public interface OAuthRepositoryCustom {

    Optional<OAuth> find(OAuthProvider provider, String providerMemberId);

    boolean existsOAuthLink(String memberKey, OAuthProvider provider);

    void deleteByMemberKey(String memberKey);
}
