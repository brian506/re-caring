package com.recaring.moremenu.controller.response;

import com.recaring.moremenu.business.MoreMenuContextType;
import com.recaring.moremenu.business.MoreMenuInfo;

import java.util.List;

public record MoreMenuResponse(
        MoreMenuContextType contextType,
        List<MoreMenuSectionResponse> sections
) {
    public static MoreMenuResponse from(MoreMenuInfo info) {
        return new MoreMenuResponse(
                info.contextType(),
                info.sections()
                        .stream()
                        .map(MoreMenuSectionResponse::from)
                        .toList()
        );
    }
}

