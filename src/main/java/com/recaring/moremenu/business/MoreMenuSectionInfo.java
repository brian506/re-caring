package com.recaring.moremenu.business;

import java.util.List;

public record MoreMenuSectionInfo(
        MoreMenuSectionKey sectionKey,
        List<MoreMenuItemInfo> items
) {
    public MoreMenuSectionInfo {
        items = List.copyOf(items);
    }
}

