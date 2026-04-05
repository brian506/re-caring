package com.recaring.security.vo;

import com.recaring.member.dataaccess.entity.MemberRole;

import java.util.Date;

public record TokenPayload(String memberKey, MemberRole role, Date date) {
}
