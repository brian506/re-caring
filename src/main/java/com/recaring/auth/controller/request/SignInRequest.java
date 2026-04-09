package com.recaring.auth.controller.request;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
