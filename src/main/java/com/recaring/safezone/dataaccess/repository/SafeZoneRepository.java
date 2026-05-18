package com.recaring.safezone.dataaccess.repository;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.custom.SafeZoneRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafeZoneRepository extends JpaRepository<SafeZone, Long>, SafeZoneRepositoryCustom {
}
