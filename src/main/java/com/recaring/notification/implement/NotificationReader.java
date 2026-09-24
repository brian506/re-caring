package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.NotificationFeedbackRepository;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.vo.FeedbackTarget;
import com.recaring.notification.vo.NotificationItem;
import com.recaring.notification.vo.NotificationSlice;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationReader {

    private final NotificationRepository notificationRepository;
    private final NotificationFeedbackRepository notificationFeedbackRepository;

    public NotificationSlice findByRecipient(String recipientMemberKey, Long cursor, int size) {
        List<Notification> notifications =
                notificationRepository.findSliceByRecipient(recipientMemberKey, cursor, size);
        Set<Long> submittedIds = findSubmittedNotificationIds(notifications);

        List<NotificationItem> fetched = notifications.stream()
                .map(notification -> NotificationItem.of(notification, submittedIds.contains(notification.getId())))
                .toList();
        return NotificationSlice.of(fetched, size);
    }

    public FeedbackTarget findFeedbackTarget(String notificationKey) {
        return notificationRepository.findByNotificationKey(notificationKey)
                .map(FeedbackTarget::from)
                .orElseThrow(() -> new AppException(ErrorType.NOTIFICATION_NOT_FOUND));
    }

    private Set<Long> findSubmittedNotificationIds(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = notifications.stream().map(Notification::getId).toList();
        return Set.copyOf(notificationFeedbackRepository.findNotificationIdsIn(ids));
    }
}
