package com.recaring.notification.dataaccess.repository;

import com.recaring.notification.dataaccess.entity.NotificationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationFeedbackRepository extends JpaRepository<NotificationFeedback, Long> {

    boolean existsByNotificationId(Long notificationId);

    @Query("select f.notificationId from NotificationFeedback f where f.notificationId in :notificationIds")
    List<Long> findNotificationIdsIn(@Param("notificationIds") Collection<Long> notificationIds);
}
