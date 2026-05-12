package com.recaring.care.dataaccess.repository;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.repository.custom.CareInvitationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CareInvitationRepository extends JpaRepository<CareInvitation, Long>,
        CareInvitationRepositoryCustom {

    Optional<CareInvitation> findByRequestKey(String requestKey);
}
