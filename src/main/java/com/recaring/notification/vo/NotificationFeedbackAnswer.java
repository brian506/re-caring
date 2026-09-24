package com.recaring.notification.vo;

import com.recaring.notification.dataaccess.entity.FeedbackAccuracy;
import com.recaring.notification.dataaccess.entity.FeedbackReason;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record NotificationFeedbackAnswer(
        FeedbackAccuracy accuracy,
        FeedbackReason reason,
        String comment
) {
    public NotificationFeedbackAnswer {
        if (accuracy == null) {
            throw new AppException(ErrorType.INVALID_NOTIFICATION_FEEDBACK);
        }
        if (accuracy.isReasonRequired() != (reason != null)) {
            throw new AppException(ErrorType.INVALID_NOTIFICATION_FEEDBACK);
        }
        comment = (comment == null || comment.isBlank()) ? null : comment.strip();
    }
}
