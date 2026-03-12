package com.recaring.domain.member.infrastructure.repository;

import com.recaring.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
