package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.NotificationRecipientType;
import com.recaring.notification.dataaccess.repository.custom.FcmDeviceTokenRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long>,
        FcmDeviceTokenRepositoryCustom {

    Optional<FcmDeviceToken> findByToken(String token);

    List<FcmDeviceToken> findAllByTokenIn(Collection<String> tokens);

    List<FcmDeviceToken> findAllByMemberKeyInAndRecipientTypeAndActiveTrue(
            Collection<String> memberKeys,
            NotificationRecipientType recipientType
    );
}
