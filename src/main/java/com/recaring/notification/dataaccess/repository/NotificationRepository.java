package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientMemberKeyOrderByCreatedAtDesc(String recipientMemberKey);
}
