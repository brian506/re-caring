package com.recaring.location.dataaccess.repository.custom;

import com.recaring.location.dataaccess.entity.GpsHistory;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.recaring.location.dataaccess.entity.QGpsHistory.gpsHistory;

public class GpsHistoryRepositoryCustomImpl extends QuerydslRepositorySupport
        implements GpsHistoryRepositoryCustom {

    protected GpsHistoryRepositoryCustomImpl() {
        super(GpsHistory.class);
    }

    @Override
    public List<GpsHistory> findByWardKeyAndDate(String wardMemberKey, LocalDate date) {
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
    public List<String> findActiveWardKeysSince(LocalDateTime since) {
        return select(gpsHistory.wardMemberKey)
                .from(gpsHistory)
                .where(gpsHistory.recordedAt.goe(since))
                .distinct()
                .fetch();
    }

    @Override
    public List<GpsHistory> findByWardKeyBetween(String wardMemberKey, LocalDateTime from, LocalDateTime to) {
        return selectFrom(gpsHistory)
                .where(
                        gpsHistory.wardMemberKey.eq(wardMemberKey),
                        gpsHistory.recordedAt.goe(from),
                        gpsHistory.recordedAt.lt(to)
                )
                .orderBy(gpsHistory.recordedAt.asc())
                .fetch();
    }
}
