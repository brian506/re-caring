package com.recaring.sms.controller.request;

import jakarta.validation.constraints.NotBlank;

public record SendCodeRequest(
        @NotBlank String phone
) {
}
