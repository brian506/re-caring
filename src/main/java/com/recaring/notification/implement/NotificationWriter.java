package com.recaring.notification.implement;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.notification.dataaccess.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationWriter {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void addAll(
            Collection<String> recipientMemberKeys,
            String eventType,
            String title,
            String body,
            Map<String, String> dataPayload
    ) {
        List<Notification> notifications = recipientMemberKeys.stream()
                .distinct()
                .map(recipientMemberKey -> Notification.builder()
                        .recipientMemberKey(recipientMemberKey)
                        .eventType(eventType)
                        .title(title)
                        .body(body)
                        .dataPayload(dataPayload)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);
    }
}
