package com.recaring.safezone.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SafeZoneRadius {
    SMALL(500),
    MEDIUM(1000),
    LARGE(1500),
    XLARGE(2000);

    private final int meters;
}
