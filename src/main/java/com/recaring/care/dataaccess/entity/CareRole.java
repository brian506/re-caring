package com.recaring.care.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum CareRole {
    PRIMARY_GUARDIAN("주보호자"),
    GUARDIAN("보호자"),
    MANAGER("관계자");

    private static final List<CareRole> GUARDIAN_ROLES = List.of(PRIMARY_GUARDIAN, GUARDIAN);

    private final String description;

    public static List<CareRole> guardianRoles() {
        return GUARDIAN_ROLES;
    }

    public boolean isGuardian() {
        return GUARDIAN_ROLES.contains(this);
    }
}
