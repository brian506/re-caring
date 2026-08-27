package com.recaring.notification.controller.response;

import com.recaring.notification.vo.SafeZoneSetting;

public record SafeZoneSettingResponse(
        boolean entryEnabled,
        boolean exitEnabled
) {
    public static SafeZoneSettingResponse from(SafeZoneSetting setting) {
        return new SafeZoneSettingResponse(setting.entryEnabled(), setting.exitEnabled());
    }
}
