package com.recaring.notification.controller.response;

import com.recaring.notification.vo.DangerZoneSetting;

public record DangerZoneSettingResponse(
        boolean entryEnabled,
        boolean exitEnabled
) {
    public static DangerZoneSettingResponse from(DangerZoneSetting setting) {
        return new DangerZoneSettingResponse(setting.entryEnabled(), setting.exitEnabled());
    }
}
