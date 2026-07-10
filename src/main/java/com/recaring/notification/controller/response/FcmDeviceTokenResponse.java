package com.recaring.notification.controller.response;

import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.care.dataaccess.entity.CarePartyRole;

public record FcmDeviceTokenResponse(
        String memberKey,
        CarePartyRole careRole,
        FcmDevicePlatform platform
) {
    public static FcmDeviceTokenResponse from(FcmDeviceToken token) {
        return new FcmDeviceTokenResponse(
                token.getMemberKey(),
                token.getCareRole(),
                token.getPlatform()
        );
    }
}
