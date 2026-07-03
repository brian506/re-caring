package com.recaring.auth.controller.request;

import jakarta.validation.constraints.NotBlank;

public record OauthLinkRequest(
        @NotBlank String accessToken
) {
}
