package com.recaring.location.dataaccess.repository;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.location.dataaccess.repository.custom.GpsHistoryRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsHistoryRepository extends JpaRepository<GpsHistory, Long>, GpsHistoryRepositoryCustom {
}
