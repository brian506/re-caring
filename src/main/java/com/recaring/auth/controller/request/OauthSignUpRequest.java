package com.recaring.auth.controller.request;

import com.recaring.auth.business.command.OAuthSignUpCommand;
import com.recaring.domain.member.dataaccess.entity.Gender;
import com.recaring.domain.member.dataaccess.entity.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OauthSignUpRequest(
        @NotBlank String providerUserId,
        @NotBlank String smsToken,
        @NotBlank String name,
        @NotNull LocalDate birth,
        @NotNull Gender gender,
        @NotNull MemberRole role
) {
    public OAuthSignUpCommand toCommand() {
        return new OAuthSignUpCommand(providerUserId, smsToken, name, birth, gender, role);
    }
}
