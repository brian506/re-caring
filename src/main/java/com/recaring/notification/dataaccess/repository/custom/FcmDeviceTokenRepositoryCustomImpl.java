package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.notification.dataaccess.entity.QFcmDeviceToken.fcmDeviceToken;

public class FcmDeviceTokenRepositoryCustomImpl extends QuerydslRepositorySupport
        implements FcmDeviceTokenRepositoryCustom {

    protected FcmDeviceTokenRepositoryCustomImpl() {
        super(FcmDeviceToken.class);
    }

    @Override
    public void deleteByMemberKey(String memberKey) {
        delete(fcmDeviceToken)
                .where(fcmDeviceToken.memberKey.eq(memberKey))
                .execute();
    }
}
