package com.recaring.care.controller.request;

import jakarta.validation.constraints.Size;

public record UpdateWardNicknameRequest(
        @Size(max = 20) String nickname
) {
}
