package com.recaring.location.dataaccess.repository.custom;

import com.recaring.location.dataaccess.entity.GpsHistory;

import java.time.LocalDate;
import java.util.List;

public interface GpsHistoryRepositoryCustom {

    List<GpsHistory> findDailyGpsHistory(String wardMemberKey, LocalDate date);

    void deleteByWardMemberKey(String wardMemberKey);
}
