package com.recaring.auth.business.command;

import com.recaring.auth.vo.EncodedPassword;
import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.NewLocalMember;
import com.recaring.auth.vo.Password;
import com.recaring.domain.member.dataaccess.entity.Gender;
import com.recaring.domain.member.dataaccess.entity.MemberRole;
import com.recaring.sms.vo.PhoneNumber;

import java.time.LocalDate;

public record SignUpCommand(
        String smsToken,
        LocalEmail email,
        Password password,
        String name,
        LocalDate birth,
        Gender gender,
        MemberRole role
) {
    public NewLocalMember toNewLocalMember(PhoneNumber phone, EncodedPassword encodedPassword) {
        return NewLocalMember.builder()
                .email(email)
                .password(encodedPassword)
                .phone(phone)
                .name(name)
                .birth(birth)
                .gender(gender)
                .role(role)
                .build();
    }
}
