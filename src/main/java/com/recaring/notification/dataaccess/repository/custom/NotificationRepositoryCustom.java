package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.dataaccess.entity.Notification;

import java.util.List;

public interface NotificationRepositoryCustom {

    List<Notification> findSliceByRecipient(String recipientMemberKey, Long cursorId, int size);
}
