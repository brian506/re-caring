package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.dataaccess.entity.NotificationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findAllByStatusAndRetryableTrueAndNextRetryAtLessThanEqual(
            NotificationDeliveryStatus status,
            LocalDateTime nextRetryAt
    );
}
