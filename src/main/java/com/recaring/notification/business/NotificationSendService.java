package com.recaring.notification.business;

import com.recaring.notification.business.command.NotificationSendCommand;
import com.recaring.notification.implement.NotificationSendManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private final NotificationSendManager notificationSendManager;

    public void send(NotificationSendCommand command) {
        notificationSendManager.sendToCareParties(
                command.guardianMemberKeys(),
                command.managerMemberKeys(),
                command.eventType(),
                command.title(),
                command.body(),
                command.dataPayload()
        );
    }
}
