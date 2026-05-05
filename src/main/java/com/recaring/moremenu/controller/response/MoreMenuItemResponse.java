package com.recaring.moremenu.controller.response;

import com.recaring.moremenu.business.MoreMenuItemInfo;
import com.recaring.moremenu.business.MoreMenuItemKey;
import com.recaring.moremenu.business.MoreMenuTargetType;

public record MoreMenuItemResponse(
        MoreMenuItemKey itemKey,
        boolean enabled,
        MoreMenuTargetType targetType,
        String target
) {
    public static MoreMenuItemResponse from(MoreMenuItemInfo info) {
        return new MoreMenuItemResponse(
                info.itemKey(),
                info.enabled(),
                info.targetType(),
                info.target()
        );
    }
}

