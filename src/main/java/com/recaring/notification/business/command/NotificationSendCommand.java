package com.recaring.notification.business.command;

import java.util.List;
import java.util.Map;

public record NotificationSendCommand(
        List<String> guardianMemberKeys,
        List<String> managerMemberKeys,
        String title,
        String body,
        Map<String, String> dataPayload,
        String eventType
) {
    public NotificationSendCommand {
        guardianMemberKeys = guardianMemberKeys == null ? List.of() : List.copyOf(guardianMemberKeys);
        managerMemberKeys = managerMemberKeys == null ? List.of() : List.copyOf(managerMemberKeys);
        dataPayload = dataPayload == null ? Map.of() : Map.copyOf(dataPayload);
    }
}
