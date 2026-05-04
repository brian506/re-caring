package com.recaring.care.vo;

import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record Ward(String memberKey) {

    public static Ward of(String memberKey, MemberRole role) {
        if (role != MemberRole.WARD) {
            throw new AppException(ErrorType.INVALID_WARD_ROLE);
        }
        return new Ward(memberKey);
    }
}
