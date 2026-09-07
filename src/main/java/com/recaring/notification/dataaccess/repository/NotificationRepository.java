package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.custom.NotificationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {
}
