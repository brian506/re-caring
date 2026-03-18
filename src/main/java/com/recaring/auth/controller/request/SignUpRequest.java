package com.recaring.auth.controller.request;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.domain.member.dataaccess.entity.Gender;
import com.recaring.domain.member.dataaccess.entity.MemberRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank String verificationToken,
        @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull LocalDate birth,
        @NotNull Gender gender,
        @NotNull MemberRole role,
        @AssertTrue Boolean isTermsOfServiceAgreed,
        @AssertTrue Boolean isLocationServiceAgreed,
        @AssertTrue Boolean isPrivacyPolicyAgreed
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(
                verificationToken,
                new LocalEmail(email),
                new Password(password),
                name,
                birth,
                gender,
                role
        );
    }
}
