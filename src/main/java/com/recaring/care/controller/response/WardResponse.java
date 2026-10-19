package com.recaring.care.controller.response;

import com.recaring.care.vo.WardInfo;
import com.recaring.care.vo.CareRole;
import com.recaring.member.vo.Gender;

import java.time.LocalDateTime;

public record WardResponse(
        String wardMemberKey,
        String wardName,
        String wardNickname,
        String wardPhone,
        Gender wardGender,
        CareRole myRole,
        String wardProfileAvatarCode,
        String designatedProfileAvatarCode,
        LocalDateTime wardJoinedAt
) {
    public static WardResponse from(WardInfo info) {
        return new WardResponse(
                info.wardMemberKey(),
                info.wardName(),
                info.wardNickname(),
                info.wardPhone(),
                info.wardGender(),
                info.myRole(),
                info.wardProfileAvatarCode(),
                info.designatedProfileAvatarCode(),
                info.wardJoinedAt()
        );
    }
}
