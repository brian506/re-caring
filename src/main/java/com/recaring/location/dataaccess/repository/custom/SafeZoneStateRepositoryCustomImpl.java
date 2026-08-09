package com.recaring.location.dataaccess.repository.custom;

import com.recaring.location.dataaccess.entity.SafeZoneState;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.location.dataaccess.entity.QSafeZoneState.safeZoneState;

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
