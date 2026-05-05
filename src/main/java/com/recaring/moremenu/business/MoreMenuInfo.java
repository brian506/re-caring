package com.recaring.moremenu.business;

import java.util.List;

public record MoreMenuInfo(
        MoreMenuContextType contextType,
        List<MoreMenuSectionInfo> sections
) {
    public MoreMenuInfo {
        sections = List.copyOf(sections);
    }
}

