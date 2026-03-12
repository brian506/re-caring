package com.recaring.auth.controller.request;

import com.recaring.auth.business.command.SignInCommand;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank String email,
        @NotBlank String password
) {
    public SignInCommand toCommand() {
        return new SignInCommand(new LocalEmail(email), new Password(password));
    }
}
