package com.recaring.auth.dataaccess.repository.custom;

public interface RefreshTokenRepositoryCustom {

    void deleteByMemberKey(String memberKey);
}
