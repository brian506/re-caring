package com.recaring.care.vo;


public record CaregiverInfo(
        String memberKey,
        String name,
        String phone,
        CareRole careRole,
        String profileAvatarCode,
        String designatedProfileAvatarCode
) {}
