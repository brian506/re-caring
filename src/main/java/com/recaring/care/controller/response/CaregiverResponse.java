package com.recaring.care.controller.response;

import com.recaring.care.vo.CaregiverInfo;
import com.recaring.care.dataaccess.entity.CareRole;

public record CaregiverResponse(
        String memberKey,
        String name,
        String phone,
        CareRole careRole
) {
    public static CaregiverResponse from(CaregiverInfo info) {
        return new CaregiverResponse(
                info.memberKey(),
                info.name(),
                info.phone(),
                info.careRole()
        );
    }
}
