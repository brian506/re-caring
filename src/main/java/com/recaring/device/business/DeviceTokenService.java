package com.recaring.device.business;

import com.recaring.device.implement.WardDeviceTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final WardDeviceTokenManager wardDeviceTokenManager;

    public String issueToken(String wardKey) {
        return wardDeviceTokenManager.issueToken(wardKey);
    }
}
