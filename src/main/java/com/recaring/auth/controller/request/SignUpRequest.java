package com.recaring.auth.controller.request;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.domain.member.Gender;
import com.recaring.domain.member.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank String verificationToken,
        @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank LocalDate birth,
        @NotBlank Gender gender,
        @NotBlank MemberRole role
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
