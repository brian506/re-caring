package com.recaring.care.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CarePartyRole {
    GUARDIAN("보호자"),
    MANAGER("관계자"),
    WARD("보호 대상자");

    private final String description;
}
