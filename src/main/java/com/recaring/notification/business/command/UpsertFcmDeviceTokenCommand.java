package com.recaring.notification.business.command;

import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.NotificationRecipientType;

public record UpsertFcmDeviceTokenCommand(
        String memberKey,
        NotificationRecipientType recipientType,
        String token,
        FcmDevicePlatform platform
) {
}
