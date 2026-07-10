package com.recaring.auth.controller.request;

import jakarta.validation.constraints.Size;

public record SignOutRequest(
        @Size(max = 512) String fcmToken
) {
}
