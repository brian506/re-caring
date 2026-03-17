package com.recaring.auth.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmailRequest(@NotBlank String name,
                           @NotNull LocalDate birth,
                           @NotBlank String phone) {
}
