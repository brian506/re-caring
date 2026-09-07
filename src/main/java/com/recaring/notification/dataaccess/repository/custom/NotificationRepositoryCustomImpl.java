package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.dataaccess.entity.Notification;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.List;

import static com.recaring.notification.dataaccess.entity.QNotification.notification;

public class NotificationRepositoryCustomImpl extends QuerydslRepositorySupport
        implements NotificationRepositoryCustom {

    protected NotificationRepositoryCustomImpl() {
        super(Notification.class);
    }

    // size + 1건을 조회해 다음 페이지 존재 여부를 판정한다 (별도 count 쿼리 없음).
    @Override
    public List<Notification> findSliceByRecipient(String recipientMemberKey, Long cursorId, int size) {
        return selectFrom(notification)
                .where(
                        notification.recipientMemberKey.eq(recipientMemberKey),
                        cursorId == null ? null : notification.id.lt(cursorId)
                )
                .orderBy(notification.id.desc())
                .limit(size + 1L)
                .fetch();
    }
}
