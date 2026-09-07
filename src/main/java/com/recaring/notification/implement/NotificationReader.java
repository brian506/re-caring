package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.repository.NotificationRepository;
import com.recaring.notification.vo.NotificationItem;
import com.recaring.notification.vo.NotificationSlice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationReader {

    private final NotificationRepository notificationRepository;

    public NotificationSlice findByRecipient(String recipientMemberKey, Long cursor, int size) {
        List<NotificationItem> fetched = notificationRepository.findSliceByRecipient(recipientMemberKey, cursor, size)
                .stream()
                .map(NotificationItem::from)
                .toList();
        return NotificationSlice.of(fetched, size);
    }
}
