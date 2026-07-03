package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.dataaccess.entity.NotificationSetting;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.notification.dataaccess.entity.QNotificationSetting.notificationSetting;

public class NotificationSettingRepositoryCustomImpl extends QuerydslRepositorySupport
        implements NotificationSettingRepositoryCustom {

    protected NotificationSettingRepositoryCustomImpl() {
        super(NotificationSetting.class);
    }

    @Override
    public void deleteByWardMemberKey(String wardMemberKey) {
        delete(notificationSetting)
                .where(notificationSetting.wardMemberKey.eq(wardMemberKey))
                .execute();
    }
}
