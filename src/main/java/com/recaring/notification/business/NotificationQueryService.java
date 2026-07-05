package com.recaring.notification.business;

import com.recaring.notification.implement.NotificationReader;
import com.recaring.notification.vo.NotificationItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationReader notificationReader;

    public List<NotificationItem> getMyNotifications(String memberKey) {
        return notificationReader.findByRecipient(memberKey);
    }
}
