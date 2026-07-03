package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.dataaccess.entity.NotificationDelivery;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.notification.dataaccess.entity.QNotificationDelivery.notificationDelivery;

public class NotificationDeliveryRepositoryCustomImpl extends QuerydslRepositorySupport
        implements NotificationDeliveryRepositoryCustom {

    protected NotificationDeliveryRepositoryCustomImpl() {
        super(NotificationDelivery.class);
    }

    @Override
    public void deleteByRecipientMemberKey(String recipientMemberKey) {
        delete(notificationDelivery)
                .where(notificationDelivery.recipientMemberKey.eq(recipientMemberKey))
                .execute();
    }
}
