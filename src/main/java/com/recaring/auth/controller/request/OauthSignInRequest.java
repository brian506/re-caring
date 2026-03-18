package com.recaring.auth.controller.request;

import jakarta.validation.constraints.NotBlank;

public record OauthSignInRequest(
        @NotBlank String accessToken
) {
}
