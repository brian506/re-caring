package com.recaring.domain.member.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
    GUARDIAN("보호자"),
    CARE_TARGET("보호 대상자");

    private final String role;

}
