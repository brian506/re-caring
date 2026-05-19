package com.recaring.notification.controller.request;

import com.recaring.notification.business.command.UpsertFcmDeviceTokenCommand;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.NotificationRecipientType;

public record UpsertFcmDeviceTokenRequest(
        String token,

        NotificationRecipientType recipientType,

        FcmDevicePlatform platform
) {
    public UpsertFcmDeviceTokenCommand toCommand(String memberKey) {
        return new UpsertFcmDeviceTokenCommand(memberKey, recipientType, token, platform);
    }
}
