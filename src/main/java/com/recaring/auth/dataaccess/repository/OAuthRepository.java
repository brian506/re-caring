package com.recaring.auth.dataaccess.repository;

import com.recaring.auth.dataaccess.entity.OAuth;
import com.recaring.auth.dataaccess.repository.custom.OAuthRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthRepository extends JpaRepository<OAuth, Long>, OAuthRepositoryCustom {
}
