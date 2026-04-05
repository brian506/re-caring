package com.recaring.auth.dataaccess.repository.custom;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.Optional;

import static com.recaring.auth.dataaccess.entity.QOAuth.oAuth;

public class OAuthRepositoryCustomImpl extends QuerydslRepositorySupport implements OAuthRepositoryCustom{

    protected OAuthRepositoryCustomImpl() {
        super(OAuth.class);
    }

    @Override
    public Optional<OAuth> findOAuthMember(OAuthProvider provider, String providerMemberId) {
        return Optional.ofNullable(
                selectFrom(oAuth)
                        .where(oAuth.provider.eq(provider),
                                oAuth.providerMemberId.eq(providerMemberId))
                        .fetchOne()
        );
    }
}
