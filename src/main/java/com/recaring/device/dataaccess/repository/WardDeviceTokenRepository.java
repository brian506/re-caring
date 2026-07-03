package com.recaring.device.dataaccess.repository;

import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.device.dataaccess.repository.custom.WardDeviceTokenRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WardDeviceTokenRepository extends JpaRepository<WardDeviceToken, Long>,
        WardDeviceTokenRepositoryCustom {
    Optional<WardDeviceToken> findByToken(String token);
    Optional<WardDeviceToken> findByWardKey(String wardKey);
}
