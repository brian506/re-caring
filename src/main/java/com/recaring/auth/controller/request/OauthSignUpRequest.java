package com.recaring.auth.controller.request;

import com.recaring.auth.business.command.OAuthSignUpCommand;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.MemberRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OauthSignUpRequest(
        @NotBlank String providerMemberId,
        @NotBlank String smsToken,
        @NotBlank String name,
        @NotNull LocalDate birth,
        @NotNull Gender gender,
        @NotNull MemberRole role,
        @AssertTrue Boolean isTermsOfServiceAgreed,
        @AssertTrue Boolean isLocationServiceAgreed,
        @AssertTrue Boolean isPrivacyPolicyAgreed
) {
    public OAuthSignUpCommand toCommand() {
        return new OAuthSignUpCommand(providerMemberId, smsToken, name, birth, gender, role);
    }
}
