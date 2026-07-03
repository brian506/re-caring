package com.recaring.device.dataaccess.repository.custom;

import com.recaring.device.dataaccess.entity.WardDeviceToken;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.device.dataaccess.entity.QWardDeviceToken.wardDeviceToken;

public class WardDeviceTokenRepositoryCustomImpl extends QuerydslRepositorySupport
        implements WardDeviceTokenRepositoryCustom {

    protected WardDeviceTokenRepositoryCustomImpl() {
        super(WardDeviceToken.class);
    }

    @Override
    public void deleteByWardKey(String wardKey) {
        delete(wardDeviceToken)
                .where(wardDeviceToken.wardKey.eq(wardKey))
                .execute();
    }
}
