package com.recaring.notification.business;

import com.recaring.notification.implement.NotificationReader;
import com.recaring.notification.vo.NotificationSlice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationReader notificationReader;

    public NotificationSlice getMyNotifications(String memberKey, Long cursor, int size) {
        return notificationReader.findByRecipient(memberKey, cursor, size);
    }
}
