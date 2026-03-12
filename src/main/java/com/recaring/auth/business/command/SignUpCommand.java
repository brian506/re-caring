package com.recaring.auth.business.command;

import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;
import com.recaring.domain.member.Gender;
import com.recaring.domain.member.MemberRole;

import java.time.LocalDate;


public record SignUpCommand(LocalEmail email, Password password, String name, LocalDate birth, Gender gender, MemberRole role) {
}
