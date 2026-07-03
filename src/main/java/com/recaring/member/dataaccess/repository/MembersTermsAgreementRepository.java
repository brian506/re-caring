package com.recaring.member.dataaccess.repository;


import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.dataaccess.repository.custom.MembersTermsAgreementRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembersTermsAgreementRepository extends JpaRepository<MembersTermsAgreement, Long>,
        MembersTermsAgreementRepositoryCustom {

    Optional<MembersTermsAgreement> findByMemberKey(String memberKey);
}
