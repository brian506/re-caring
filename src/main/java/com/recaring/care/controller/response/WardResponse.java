package com.recaring.care.controller.response;

import com.recaring.care.vo.WardInfo;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.member.dataaccess.entity.Gender;

public record WardResponse(
        String wardMemberKey,
        String wardName,
        String wardPhone,
        Gender wardGender,
        CareRole myRole
) {
    public static WardResponse from(WardInfo info) {
        return new WardResponse(
                info.wardMemberKey(),
                info.wardName(),
                info.wardPhone(),
                info.wardGender(),
                info.myRole()
        );
    }
}
