package com.recaring.notification.vo;

import com.recaring.care.vo.CarePartyRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;

public record FcmDeviceTokenInfo(
        String memberKey,
        CarePartyRole careRole,
        String token,
        FcmDevicePlatform platform
) {
    public static FcmDeviceTokenInfo from(FcmDeviceToken entity) {
        return new FcmDeviceTokenInfo(
                entity.getMemberKey(),
                entity.getCareRole(),
                entity.getToken(),
                entity.getPlatform()
        );
    }
}
