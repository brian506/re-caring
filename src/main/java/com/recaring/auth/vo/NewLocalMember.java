package com.recaring.auth.vo;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.sms.vo.PhoneNumber;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record NewLocalMember(LocalEmail email, EncodedPassword password, PhoneNumber phone, String name, LocalDate birth, Gender gender, MemberRole role) {
}
