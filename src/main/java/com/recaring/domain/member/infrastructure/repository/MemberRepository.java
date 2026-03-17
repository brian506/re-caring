package com.recaring.domain.member.infrastructure.repository;

import com.recaring.domain.member.Member;
import com.recaring.domain.member.infrastructure.repository.custom.MemberRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByMemberKey(String memberKey);

    Optional<Member> findByPhone(String phone);
}
