package com.recaring.auth.controller.request;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.domain.member.Gender;
import com.recaring.domain.member.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank String verificationToken,
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull LocalDate birth,
        @NotNull Gender gender,
        @NotNull MemberRole role
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(
                verificationToken,
                new LocalEmail(email),
                new Password(password),
                name,
                birth,
                gender,
                role);
    }
}
