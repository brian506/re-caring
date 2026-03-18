package com.recaring.auth.dataaccess.repository;

import com.recaring.auth.dataaccess.entity.LocalAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalAuthRepository extends JpaRepository<LocalAuth, Long> {

    Optional<LocalAuth> findByEmail(String email);

    Optional<LocalAuth> findByMemberKey(String memberKey);

    boolean existsByEmail(String email);
}
