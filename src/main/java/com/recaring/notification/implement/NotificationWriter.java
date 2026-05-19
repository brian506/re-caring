package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.notification.dataaccess.entity.NotificationDeliveryStatus;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationWriter {

    private static final String NO_ACTIVE_TOKEN = "NO_ACTIVE_TOKEN";
    private static final String DELIVERY_FAILED = "DELIVERY_FAILED";

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification addRequested(String eventType, String title, String body, String dataPayload) {
        return notificationRepository.save(Notification.builder()
                .eventType(eventType)
                .title(title)
                .body(body)
                .dataPayload(dataPayload)
                .build());
    }

    @Transactional
    public void markFailedForNoActiveToken(Notification notification) {
        notification.markFailed(NO_ACTIVE_TOKEN, "No active FCM device token was found.");
        notificationRepository.save(notification);
    }

    @Transactional
    public void completeByDeliveries(Notification notification, List<NotificationDelivery> deliveries) {
        long sentCount = deliveries.stream()
                .filter(delivery -> delivery.getStatus() == NotificationDeliveryStatus.SENT)
                .count();
        long failedCount = deliveries.stream()
                .filter(delivery -> delivery.getStatus() == NotificationDeliveryStatus.FAILED)
                .count();

        if (sentCount > 0 && failedCount == 0) {
            notification.markSent();
            notificationRepository.save(notification);
            return;
        }
        if (sentCount > 0) {
            notification.markPartial(DELIVERY_FAILED, failedCount + " deliveries failed.");
            notificationRepository.save(notification);
            return;
        }
        notification.markFailed(DELIVERY_FAILED, "All deliveries failed.");
        notificationRepository.save(notification);
    }
}
