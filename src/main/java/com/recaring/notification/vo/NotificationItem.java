package com.recaring.notification.vo;

import com.recaring.location.vo.DetectionType;
import com.recaring.notification.dataaccess.entity.Notification;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationItem(
        Long id,
        String notificationKey,
        String eventType,
        String title,
        String body,
        Map<String, String> dataPayload,
        LocalDateTime createdAt,
        boolean feedbackEligible,
        boolean feedbackSubmitted
) {
    public static NotificationItem of(Notification notification, boolean feedbackSubmitted) {
        return new NotificationItem(
                notification.getId(),
                notification.getNotificationKey(),
                notification.getEventType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getDataPayload(),
                notification.getCreatedAt(),
                DetectionType.find(notification.getEventType()).isPresent(),
                feedbackSubmitted
        );
    }
}
