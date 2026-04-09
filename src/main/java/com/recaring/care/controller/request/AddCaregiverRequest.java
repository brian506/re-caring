package com.recaring.care.controller.request;

import jakarta.validation.constraints.NotBlank;

public record AddCaregiverRequest(
        @NotBlank String phoneNumber,
        @NotBlank String wardMemberKey
) {
}
