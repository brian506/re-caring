package com.recaring.location.dataaccess.repository.custom;

import com.recaring.location.dataaccess.entity.GpsHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface GpsHistoryRepositoryCustom {

    List<GpsHistory> findByWardKeyAndDate(String wardMemberKey, LocalDate date);

    List<String> findActiveWardKeysSince(LocalDateTime since);

    List<GpsHistory> findByWardKeyBetween(String wardMemberKey, LocalDateTime from, LocalDateTime to);
}
