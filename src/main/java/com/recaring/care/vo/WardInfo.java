package com.recaring.care.vo;

import com.recaring.member.vo.Gender;

import java.time.LocalDateTime;

public record WardInfo(
        String wardMemberKey,
        String wardName,
        String wardNickname,
        String wardPhone,
        Gender wardGender,
        CareRole myRole,
        String wardProfileAvatarCode,
        String designatedProfileAvatarCode,
        LocalDateTime wardJoinedAt
) {}
