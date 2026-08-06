package com.recaring.notification.business;

import com.recaring.care.dataaccess.entity.CarePartyRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.implement.fcm.FcmDeviceTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmDeviceTokenService {

    private final FcmDeviceTokenManager fcmDeviceTokenManager;

    public FcmDeviceToken upsert(String memberKey, String token, CarePartyRole careRole, FcmDevicePlatform platform) {
        return fcmDeviceTokenManager.upsert(memberKey, token, careRole, platform);
    }

    public void delete(String token) {
        fcmDeviceTokenManager.deleteByToken(token);
    }
}
