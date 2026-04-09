package com.recaring.care.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CareRole {
    GUARDIAN("보호자"),
    MANAGER("관계자");

    private final String description;
}
