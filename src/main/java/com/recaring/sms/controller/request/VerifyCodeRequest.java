package com.recaring.sms.controller.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyCodeRequest(
        @NotBlank String phone,
        @NotBlank String code
) {
}
