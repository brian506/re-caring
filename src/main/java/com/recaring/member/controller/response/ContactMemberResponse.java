package com.recaring.member.controller.response;

import com.recaring.member.vo.MemberInfo;
import com.recaring.member.vo.MemberRole;

public record ContactMemberResponse(
        String memberKey,
        String name,
        String phone,
        MemberRole role
) {
    public static ContactMemberResponse from(MemberInfo member) {
        return new ContactMemberResponse(
                member.memberKey(),
                member.name(),
                member.phone(),
                member.role()
        );
    }
}
