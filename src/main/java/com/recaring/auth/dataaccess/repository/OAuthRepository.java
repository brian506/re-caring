package com.recaring.auth.dataaccess.repository;

import com.recaring.auth.dataaccess.repository.custom.OAuthRepositoryCustom;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.auth.dataaccess.entity.OAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthRepository extends JpaRepository<OAuth, Long>, OAuthRepositoryCustom {

    Optional<OAuth> findByProviderAndProviderMemberId(OAuthProvider provider, String providerMemberId);
}
