package com.recaring.notification.business;

import com.recaring.notification.business.command.UpsertFcmDeviceTokenCommand;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.notification.implement.FcmDeviceTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmDeviceTokenService {

    private final FcmDeviceTokenManager fcmDeviceTokenManager;

    public FcmDeviceToken upsert(UpsertFcmDeviceTokenCommand command) {
        return fcmDeviceTokenManager.upsert(command);
    }
}
