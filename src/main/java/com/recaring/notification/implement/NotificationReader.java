package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.vo.NotificationItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationReader {

    private final NotificationRepository notificationRepository;

    public List<NotificationItem> findByRecipient(String recipientMemberKey) {
        return notificationRepository.findByRecipientMemberKeyOrderByCreatedAtDesc(recipientMemberKey)
                .stream()
                .map(NotificationItem::from)
                .toList();
    }
}
