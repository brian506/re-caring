package com.recaring.notification.business;

import com.recaring.care.vo.CarePartyRole;
import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.notification.implement.fcm.FcmDeviceTokenManager;
import com.recaring.notification.vo.FcmDeviceTokenInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmDeviceTokenService {

    private final FcmDeviceTokenManager fcmDeviceTokenManager;

    public FcmDeviceTokenInfo upsert(String memberKey, String token, CarePartyRole careRole, FcmDevicePlatform platform) {
        return fcmDeviceTokenManager.upsert(memberKey, token, careRole, platform);
    }

    public void delete(String token) {
        fcmDeviceTokenManager.deleteByToken(token);
    }
}
