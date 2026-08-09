package com.recaring.location.dataaccess.repository.custom;

import com.recaring.location.dataaccess.entity.GpsHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GpsHistoryRepositoryCustom {

    List<GpsHistory> findDailyGpsHistory(String wardMemberKey, LocalDate date);

    Optional<GpsHistory> findLatest(String wardMemberKey);

    void deleteByWardMemberKey(String wardMemberKey);
}
