package com.recaring.member.controller.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SearchByPhonesRequest(
        @NotEmpty List<String> phones
) {
}
