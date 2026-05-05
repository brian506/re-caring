package com.recaring.moremenu.business;

public record MoreMenuItemInfo(
        MoreMenuItemKey itemKey,
        boolean enabled,
        MoreMenuTargetType targetType,
        String target
) {
}

