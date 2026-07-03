package com.recaring.auth.dataaccess.repository.custom;

import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.auth.dataaccess.entity.QRefreshToken.refreshToken;

public class RefreshTokenRepositoryCustomImpl extends QuerydslRepositorySupport
        implements RefreshTokenRepositoryCustom {

    protected RefreshTokenRepositoryCustomImpl() {
        super(RefreshToken.class);
    }

    @Override
    public void deleteByMemberKey(String memberKey) {
        delete(refreshToken)
                .where(refreshToken.memberKey.eq(memberKey))
                .execute();
    }
}
