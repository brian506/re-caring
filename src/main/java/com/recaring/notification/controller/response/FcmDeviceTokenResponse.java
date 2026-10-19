package com.recaring.notification.controller.response;

import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.care.vo.CarePartyRole;
import com.recaring.notification.vo.FcmDeviceTokenInfo;

public record FcmDeviceTokenResponse(
        String memberKey,
        CarePartyRole careRole,
        FcmDevicePlatform platform
) {
    public static FcmDeviceTokenResponse from(FcmDeviceTokenInfo token) {
        return new FcmDeviceTokenResponse(
                token.memberKey(),
                token.careRole(),
                token.platform()
        );
    }
}
