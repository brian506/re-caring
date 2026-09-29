package com.recaring.auth.controller.request;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank String phone,
        @NotBlank String password
) {
}
