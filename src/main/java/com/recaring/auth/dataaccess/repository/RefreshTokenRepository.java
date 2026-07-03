package com.recaring.auth.dataaccess.repository;

import com.recaring.auth.dataaccess.entity.RefreshToken;
import com.recaring.auth.dataaccess.repository.custom.RefreshTokenRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepositoryCustom {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);
}
