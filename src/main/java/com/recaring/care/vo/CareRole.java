package com.recaring.care.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CareRole {
    GUARDIAN("보호자");

    private final String description;
}
