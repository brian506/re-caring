package com.recaring.notification.business;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.implement.FcmDeviceTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmDeviceTokenService {

    private final FcmDeviceTokenManager fcmDeviceTokenManager;

    public FcmDeviceToken upsert(String memberKey, String token, CareRole careRole, FcmDevicePlatform platform) {
        return fcmDeviceTokenManager.upsert(memberKey, token, careRole, platform);
    }
}
