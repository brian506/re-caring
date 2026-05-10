package com.recaring.safezone.dataaccess.repository.custom;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.List;
import java.util.Optional;

import static com.recaring.safezone.dataaccess.entity.QSafeZone.safeZone;

public class SafeZoneRepositoryCustomImpl extends QuerydslRepositorySupport
        implements SafeZoneRepositoryCustom {

    protected SafeZoneRepositoryCustomImpl() {
        super(SafeZone.class);
    }

    @Override
    public List<SafeZone> findAllByWardMemberKey(String wardMemberKey) {
        return selectFrom(safeZone)
                .where(safeZone.wardMemberKey.eq(wardMemberKey))
                .orderBy(safeZone.createdAt.asc())
                .fetch();
    }

    @Override
    public Optional<SafeZone> findBySafeZoneKey(String safeZoneKey) {
        return Optional.ofNullable(
                selectFrom(safeZone)
                        .where(safeZone.safeZoneKey.eq(safeZoneKey))
                        .fetchOne()
        );
    }
}
