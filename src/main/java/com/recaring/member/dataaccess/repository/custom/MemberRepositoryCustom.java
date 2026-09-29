package com.recaring.member.dataaccess.repository.custom;

import com.recaring.member.dataaccess.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<Member> findForUpdate(String memberKey);

    List<Member> findByPhones(List<String> phones);

    void deleteByMemberKey(String memberKey);
}
