package com.recaring.member.dataaccess.repository.custom;

import com.recaring.member.dataaccess.entity.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<Member> findAccount(String name, LocalDate birth, String phone);

    Optional<Member> findForUpdate(String memberKey);

    List<Member> findByPhones(List<String> phones);

    void deleteByMemberKey(String memberKey);
}
