package com.recaring.care.controller.request;

import jakarta.validation.constraints.NotBlank;

public record AddWardRequest(
        @NotBlank String phoneNumber
) {
}
