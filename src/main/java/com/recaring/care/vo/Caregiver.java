package com.recaring.care.vo;

import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record Caregiver(String memberKey) {

    public static Caregiver of(String memberKey, MemberRole role) {
        if (role != MemberRole.GUARDIAN) {
            throw new AppException(ErrorType.INVALID_CAREGIVER_ROLE);
        }
        return new Caregiver(memberKey);
    }
}
