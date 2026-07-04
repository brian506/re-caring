package com.recaring.member.controller.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateMyInfoRequest(
        @Size(max = 20) String name,
        @Past LocalDate birth,
        String currentPassword,
        String newPassword
) {
}
