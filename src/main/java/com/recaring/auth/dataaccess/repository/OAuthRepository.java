package com.recaring.auth.dataaccess.repository;

import com.recaring.auth.dataaccess.repository.custom.OAuthRepositoryCustom;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.dataaccess.entity.OAuth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthRepository extends JpaRepository<OAuth, Long>, OAuthRepositoryCustom {

    boolean existsByMemberKeyAndProvider(String memberKey, OAuthProvider provider);
}
