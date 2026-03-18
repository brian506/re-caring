package com.recaring.domain.member.dataaccess.repository;

import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.domain.member.dataaccess.repository.custom.MemberRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    Optional<Member> findByMemberKey(String memberKey);

    Optional<Member> findByPhone(String phone);
}
