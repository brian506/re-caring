package com.recaring.notification.controller.request;

import jakarta.validation.constraints.NotNull;

public record UpdateDangerZoneNotificationSettingRequest(
        @NotNull Boolean entryEnabled,
        @NotNull Boolean exitEnabled
) {
}
