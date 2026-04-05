package com.recaring.member.controller.response;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;

public record ContactMemberResponse(
        String memberKey,
        String name,
        String phone,
        MemberRole role
) {
    public static ContactMemberResponse from(Member member) {
        return new ContactMemberResponse(
                member.getMemberKey(),
                member.getName(),
                member.getPhone(),
                member.getRole()
        );
    }
}
