package com.recaring.location.dataaccess.repository.custom;

import com.recaring.location.dataaccess.entity.LocationSetting;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.location.dataaccess.entity.QLocationSetting.locationSetting;

public class LocationSettingRepositoryCustomImpl extends QuerydslRepositorySupport
        implements LocationSettingRepositoryCustom {

    protected LocationSettingRepositoryCustomImpl() {
        super(LocationSetting.class);
    }

    @Override
    public void deleteByMemberKey(String memberKey) {
        delete(locationSetting)
                .where(locationSetting.wardMemberKey.eq(memberKey)
                        .or(locationSetting.caregiverMemberKey.eq(memberKey)))
                .execute();
    }

    @Override
    public void deleteByRelationship(String wardMemberKey, String caregiverMemberKey) {
        delete(locationSetting)
                .where(locationSetting.wardMemberKey.eq(wardMemberKey)
                        .and(locationSetting.caregiverMemberKey.eq(caregiverMemberKey)))
                .execute();
    }
}
