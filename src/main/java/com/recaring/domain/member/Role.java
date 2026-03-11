package com.recaring.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    GUARDIAN("보호자"),
    CARE_TARGET("보호 대상자");

    private final String role;

}
