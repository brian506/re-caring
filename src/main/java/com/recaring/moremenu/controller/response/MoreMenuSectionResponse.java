package com.recaring.moremenu.controller.response;

import com.recaring.moremenu.business.MoreMenuSectionInfo;
import com.recaring.moremenu.business.MoreMenuSectionKey;

import java.util.List;

public record MoreMenuSectionResponse(
        MoreMenuSectionKey sectionKey,
        List<MoreMenuItemResponse> items
) {
    public static MoreMenuSectionResponse from(MoreMenuSectionInfo info) {
        return new MoreMenuSectionResponse(
                info.sectionKey(),
                info.items()
                        .stream()
                        .map(MoreMenuItemResponse::from)
                        .toList()
        );
    }
}

