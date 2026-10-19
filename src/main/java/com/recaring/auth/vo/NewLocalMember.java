package com.recaring.auth.vo;

import com.recaring.member.vo.Gender;
import com.recaring.member.vo.MemberRole;
import com.recaring.sms.vo.PhoneNumber;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record NewLocalMember(EncodedPassword password, PhoneNumber phone, String name, LocalDate birth, Gender gender, MemberRole role) {
}
