package com.recaring.notification.controller.response;

import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.dataaccess.entity.NotificationRecipientType;

public record FcmDeviceTokenResponse(
        Long id,
        String memberKey,
        NotificationRecipientType recipientType,
        FcmDevicePlatform platform,
        boolean active
) {
    public static FcmDeviceTokenResponse from(FcmDeviceToken token) {
        return new FcmDeviceTokenResponse(
                token.getId(),
                token.getMemberKey(),
                token.getRecipientType(),
                token.getPlatform(),
                token.isActive()
        );
    }
}
