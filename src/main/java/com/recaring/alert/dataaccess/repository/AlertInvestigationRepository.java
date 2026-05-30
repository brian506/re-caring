package com.recaring.alert.dataaccess.repository;

import com.recaring.alert.dataaccess.entity.AlertInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertInvestigationRepository extends JpaRepository<AlertInvestigation, Long> {

    Optional<AlertInvestigation> findByFingerprint(String fingerprint);
}
