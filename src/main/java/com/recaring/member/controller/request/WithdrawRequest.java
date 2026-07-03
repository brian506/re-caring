package com.recaring.member.controller.request;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(@NotBlank String password) {
}
