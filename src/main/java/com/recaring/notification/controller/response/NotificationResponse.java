package com.recaring.notification.controller.response;

import com.recaring.notification.vo.NotificationItem;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
        String notificationKey,
        String eventType,
        String title,
        String body,
        Map<String, String> dataPayload,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(NotificationItem item) {
        return new NotificationResponse(
                item.notificationKey(),
                item.eventType(),
                item.title(),
                item.body(),
                item.dataPayload(),
                item.createdAt()
        );
    }
}
