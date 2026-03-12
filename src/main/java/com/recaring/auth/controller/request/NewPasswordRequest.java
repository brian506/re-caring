package com.recaring.auth.controller.request;

import jakarta.validation.constraints.NotBlank;

public record NewPasswordRequest(@NotBlank String smsToken, @NotBlank String password) {
}
