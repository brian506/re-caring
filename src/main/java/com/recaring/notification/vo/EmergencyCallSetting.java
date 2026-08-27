package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.NotificationSetting;

public record EmergencyCallSetting(boolean enabled) {

    public static EmergencyCallSetting from(NotificationSetting setting) {
        return new EmergencyCallSetting(setting.isEmergencyCallEnabled());
    }
}
