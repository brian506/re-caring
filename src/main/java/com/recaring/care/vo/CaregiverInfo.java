package com.recaring.care.vo;

import com.recaring.care.dataaccess.entity.CareRole;

public record CaregiverInfo(
        String memberKey,
        String name,
        String phone,
        CareRole careRole,
        String profileAvatarCode,
        String designatedProfileAvatarCode
) {}
