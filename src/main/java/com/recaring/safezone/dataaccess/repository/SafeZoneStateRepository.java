package com.recaring.safezone.dataaccess.repository;

import com.recaring.safezone.dataaccess.entity.SafeZoneState;
import com.recaring.safezone.dataaccess.repository.custom.SafeZoneStateRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SafeZoneStateRepository extends JpaRepository<SafeZoneState, Long>,
        SafeZoneStateRepositoryCustom {
    Optional<SafeZoneState> findByWardMemberKey(String wardMemberKey);
}
