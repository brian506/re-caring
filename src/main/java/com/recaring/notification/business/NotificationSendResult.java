package com.recaring.notification.business;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.dataaccess.entity.NotificationDeliveryStatus;
import com.recaring.notification.dataaccess.entity.NotificationStatus;

import java.util.List;

public record NotificationSendResult(
        String notificationKey,
        NotificationStatus status,
        long requestedCount,
        long sentCount,
        long failedCount
) {
    public static NotificationSendResult from(Notification notification, List<NotificationDelivery> deliveries) {
        long sent = deliveries.stream()
                .filter(delivery -> delivery.getStatus() == NotificationDeliveryStatus.SENT)
                .count();
        long failed = deliveries.stream()
                .filter(delivery -> delivery.getStatus() == NotificationDeliveryStatus.FAILED)
                .count();
        return new NotificationSendResult(
                notification.getNotificationKey(),
                notification.getStatus(),
                deliveries.size(),
                sent,
                failed
        );
    }
}
