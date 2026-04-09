package com.recaring.member.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
    GUARDIAN("보호자"),
    WARD("보호 대상자");

    private final String role;

}
