package com.recaring.member.dataaccess.repository;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.custom.MemberRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    Optional<Member> findByMemberKey(String memberKey);

    Optional<Member> findByPhone(String phone);

    List<Member> findAllByPhoneIn(List<String> phones);

    List<Member> findAllByMemberKeyIn(List<String> memberKeys);
}
