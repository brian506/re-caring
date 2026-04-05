package com.recaring.care.dataaccess.repository;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.repository.custom.CareInvitationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareInvitationRepository extends JpaRepository<CareInvitation, Long>,
        CareInvitationRepositoryCustom {
}
