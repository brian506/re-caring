package com.recaring.member.dataaccess.repository;

import com.recaring.member.dataaccess.entity.MemberWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberWithdrawalRepository extends JpaRepository<MemberWithdrawal, Long> {
}
