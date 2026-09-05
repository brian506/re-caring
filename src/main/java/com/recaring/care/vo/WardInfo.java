package com.recaring.care.vo;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.member.dataaccess.entity.Gender;

public record WardInfo(
        String wardMemberKey,
        String wardName,
        String wardNickname,
        String wardPhone,
        Gender wardGender,
        CareRole myRole
) {}
