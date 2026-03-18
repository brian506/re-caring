package com.recaring.domain.member.dataaccess.repository.custom;

import com.recaring.domain.member.dataaccess.entity.Member;

import java.time.LocalDate;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<Member> findAccount(String name, LocalDate birth, String phone);
}
