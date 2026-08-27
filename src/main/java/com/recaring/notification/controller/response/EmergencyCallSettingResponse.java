package com.recaring.notification.controller.response;

import com.recaring.notification.vo.EmergencyCallSetting;

public record EmergencyCallSettingResponse(boolean enabled) {

    public static EmergencyCallSettingResponse from(EmergencyCallSetting setting) {
        return new EmergencyCallSettingResponse(setting.enabled());
    }
}
