package com.recaring.security.vo;

import com.recaring.domain.member.Role;

import java.util.Date;

public record TokenPayload(String memberKey, Role role, Date date) {
}
