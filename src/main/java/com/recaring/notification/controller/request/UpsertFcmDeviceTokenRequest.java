package com.recaring.notification.controller.request;

import com.recaring.notification.dataaccess.entity.FcmDevicePlatform;
import com.recaring.care.dataaccess.entity.CareRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertFcmDeviceTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotNull CareRole careRole,
        @NotNull FcmDevicePlatform platform
) {
}
