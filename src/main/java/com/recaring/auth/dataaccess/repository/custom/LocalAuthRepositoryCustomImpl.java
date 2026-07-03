package com.recaring.auth.dataaccess.repository.custom;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.auth.dataaccess.entity.QLocalAuth.localAuth;

public class LocalAuthRepositoryCustomImpl extends QuerydslRepositorySupport
        implements LocalAuthRepositoryCustom {

    protected LocalAuthRepositoryCustomImpl() {
        super(LocalAuth.class);
    }

    @Override
    public void deleteByMemberKey(String memberKey) {
        delete(localAuth)
                .where(localAuth.memberKey.eq(memberKey))
                .execute();
    }
}
