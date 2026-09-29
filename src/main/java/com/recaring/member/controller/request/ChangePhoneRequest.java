package com.recaring.member.controller.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePhoneRequest(@NotBlank String smsToken, @NotBlank String password) {
}
