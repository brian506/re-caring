package com.recaring.domain.member.infrastructure.repository.custom;

import com.recaring.domain.member.Member;

import java.time.LocalDate;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<Member> findByNameAndBirthAndPhone(String name, LocalDate birth, String phone);
}
