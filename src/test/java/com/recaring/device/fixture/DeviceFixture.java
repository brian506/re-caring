package com.recaring.device.fixture;

import com.recaring.device.dataaccess.entity.WardDeviceToken;

public class DeviceFixture {

    public static final String WARD_KEY = "ward-key-001";

    public static WardDeviceToken createDeviceToken(String wardKey) {
        return WardDeviceToken.builder()
                .wardKey(wardKey)
                .build();
    }
}
