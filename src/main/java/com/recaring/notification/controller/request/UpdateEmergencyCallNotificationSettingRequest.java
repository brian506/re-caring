package com.recaring.notification.controller.request;

import jakarta.validation.constraints.NotNull;

public record UpdateEmergencyCallNotificationSettingRequest(
        @NotNull Boolean enabled
) {
}
