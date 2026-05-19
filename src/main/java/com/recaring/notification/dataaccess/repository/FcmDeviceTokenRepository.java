package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.NotificationRecipientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FcmDeviceTokenRepository extends JpaRepository<FcmDeviceToken, Long> {

    Optional<FcmDeviceToken> findByToken(String token);

    List<FcmDeviceToken> findAllByMemberKeyInAndRecipientTypeAndActiveTrue(
            Collection<String> memberKeys,
            NotificationRecipientType recipientType
    );
}
