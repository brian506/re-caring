package com.recaring.care.vo;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record Caregiver(String memberKey) {

    public static Caregiver from(Member member) {
        if (member.getRole() != MemberRole.GUARDIAN) {
            throw new AppException(ErrorType.INVALID_CAREGIVER_ROLE);
        }
        return new Caregiver(member.getMemberKey());
    }
}
