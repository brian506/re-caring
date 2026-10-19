package com.recaring.safezone.dataaccess.repository.custom;

import com.recaring.safezone.dataaccess.entity.SafeZoneState;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.safezone.dataaccess.entity.QSafeZoneState.safeZoneState;

public class SafeZoneStateRepositoryCustomImpl extends QuerydslRepositorySupport
        implements SafeZoneStateRepositoryCustom {

    protected SafeZoneStateRepositoryCustomImpl() {
        super(SafeZoneState.class);
    }

    @Override
    public void deleteByWardMemberKey(String wardMemberKey) {
        delete(safeZoneState)
                .where(safeZoneState.wardMemberKey.eq(wardMemberKey))
                .execute();
    }
}
