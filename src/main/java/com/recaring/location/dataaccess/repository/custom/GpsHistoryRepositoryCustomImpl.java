package com.recaring.location.dataaccess.repository.custom;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.time.LocalDate;
import java.util.List;

import static com.recaring.location.dataaccess.entity.QGpsHistory.gpsHistory;

public class GpsHistoryRepositoryCustomImpl extends QuerydslRepositorySupport
        implements GpsHistoryRepositoryCustom {

    protected GpsHistoryRepositoryCustomImpl() {
        super(GpsHistory.class);
    }

    @Override
    public List<GpsHistory> findDailyGpsHistory(String wardMemberKey, LocalDate date) {
        return selectFrom(gpsHistory)
                .where(
                        gpsHistory.wardMemberKey.eq(wardMemberKey),
                        gpsHistory.recordedAt.goe(date.atStartOfDay()),
                        gpsHistory.recordedAt.lt(date.plusDays(1).atStartOfDay())
                )
                .orderBy(gpsHistory.recordedAt.asc())
                .fetch();
    }

    @Override
    public void deleteByWardMemberKey(String wardMemberKey) {
        delete(gpsHistory)
                .where(gpsHistory.wardMemberKey.eq(wardMemberKey))
                .execute();
    }
}
