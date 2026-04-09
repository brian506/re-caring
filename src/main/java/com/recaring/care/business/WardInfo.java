package com.recaring.care.business;

import com.recaring.care.dataaccess.entity.CareRole;

public record WardInfo(
        String wardMemberKey,
        String wardName,
        String wardPhone,
        CareRole myRole
) {
}
