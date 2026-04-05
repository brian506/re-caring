package com.recaring.care.vo;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record Ward(String memberKey) {

    public static Ward from(Member member) {
        if (member.getRole() != MemberRole.WARD) {
            throw new AppException(ErrorType.INVALID_WARD_ROLE);
        }
        return new Ward(member.getMemberKey());
    }
}
