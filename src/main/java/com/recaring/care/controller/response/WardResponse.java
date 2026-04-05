package com.recaring.care.controller.response;

import com.recaring.care.business.WardInfo;
import com.recaring.care.dataaccess.entity.CareRole;

public record WardResponse(
        String wardMemberKey,
        String wardName,
        String wardPhone,
        CareRole myRole
) {
    public static WardResponse from(WardInfo info) {
        return new WardResponse(
                info.wardMemberKey(),
                info.wardName(),
                info.wardPhone(),
                info.myRole()
        );
    }
}
