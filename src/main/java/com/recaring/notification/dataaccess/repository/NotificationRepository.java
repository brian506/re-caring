package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByNotificationKey(String notificationKey);
}
