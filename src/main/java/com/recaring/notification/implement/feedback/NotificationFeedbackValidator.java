package com.recaring.notification.implement.feedback;

import com.recaring.notification.dataaccess.repository.NotificationFeedbackRepository;
import com.recaring.notification.vo.FeedbackTarget;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFeedbackValidator {

    private final NotificationFeedbackRepository notificationFeedbackRepository;

    public void validateSubmittable(FeedbackTarget target, String memberKey) {
        if (!target.isRecipient(memberKey)) {
            throw new AppException(ErrorType.NOTIFICATION_ACCESS_DENIED);
        }
        if (!target.isFeedbackEligible()) {
            throw new AppException(ErrorType.NOTIFICATION_FEEDBACK_NOT_ELIGIBLE);
        }
        if (notificationFeedbackRepository.existsByNotificationId(target.notificationId())) {
            throw new AppException(ErrorType.NOTIFICATION_FEEDBACK_ALREADY_SUBMITTED);
        }
    }
}
