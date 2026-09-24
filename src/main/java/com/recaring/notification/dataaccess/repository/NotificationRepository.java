package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.custom.NotificationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {

    Optional<Notification> findByNotificationKey(String notificationKey);
}
