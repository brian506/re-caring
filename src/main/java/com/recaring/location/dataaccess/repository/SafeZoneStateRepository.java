package com.recaring.location.dataaccess.repository;

import com.recaring.location.dataaccess.entity.SafeZoneState;
import com.recaring.location.dataaccess.repository.custom.SafeZoneStateRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SafeZoneStateRepository extends JpaRepository<SafeZoneState, Long>,
        SafeZoneStateRepositoryCustom {
    Optional<SafeZoneState> findByWardMemberKey(String wardMemberKey);
}
