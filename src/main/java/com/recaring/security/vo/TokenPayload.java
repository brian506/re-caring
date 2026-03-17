package com.recaring.security.vo;

import com.recaring.domain.member.MemberRole;

import java.util.Date;

public record TokenPayload(String memberKey, MemberRole role, Date date) {
}
