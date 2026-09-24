package com.recaring.notification.business;

import com.recaring.notification.implement.NotificationReader;
import com.recaring.notification.implement.feedback.NotificationFeedbackManager;
import com.recaring.notification.vo.NotificationFeedbackAnswer;
import com.recaring.notification.vo.NotificationSlice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationReader notificationReader;
    private final NotificationFeedbackManager notificationFeedbackManager;

    public NotificationSlice getMyNotifications(String memberKey, Long cursor, int size) {
        return notificationReader.findByRecipient(memberKey, cursor, size);
    }

    public void submitFeedback(String memberKey, String notificationKey, NotificationFeedbackAnswer answer) {
        notificationFeedbackManager.submit(memberKey, notificationKey, answer);
    }
}
