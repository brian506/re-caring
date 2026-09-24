package com.recaring.notification.implement.feedback;

import com.recaring.location.implement.detection.AnomalyDetectionManager;
import com.recaring.notification.dataaccess.entity.NotificationFeedback;
import com.recaring.notification.dataaccess.repository.NotificationFeedbackRepository;
import com.recaring.notification.implement.NotificationReader;
import com.recaring.notification.vo.FeedbackTarget;
import com.recaring.notification.vo.NotificationFeedbackAnswer;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFeedbackManager {

    private final NotificationReader notificationReader;
    private final NotificationFeedbackValidator notificationFeedbackValidator;
    private final NotificationFeedbackRepository notificationFeedbackRepository;
    private final AnomalyDetectionManager anomalyDetectionManager;

    @Transactional
    public void submit(String memberKey, String notificationKey, NotificationFeedbackAnswer answer) {
        FeedbackTarget target = notificationReader.findFeedbackTarget(notificationKey);
        notificationFeedbackValidator.validateSubmittable(target, memberKey);

        Long anomalyDetectionId = resolveAnomalyDetectionId(target);
        notificationFeedbackRepository.save(NotificationFeedback.builder()
                .notificationId(target.notificationId())
                .anomalyDetectionId(anomalyDetectionId)
                .accuracy(answer.accuracy())
                .reason(answer.reason())
                .comment(answer.comment())
                .build());

        log.info("[알림 피드백 : 제출 완료]: notificationKey={} | accuracy={} | reason={} | anomalyDetectionId={}",
                notificationKey, answer.accuracy(), answer.reason(), anomalyDetectionId);
    }

    private Long resolveAnomalyDetectionId(FeedbackTarget target) {
        if (!target.hasDetectionKeys()) {
            log.warn("[알림 피드백 : 탐지 역조회 키 없음]: notificationId={} | eventType={}",
                    target.notificationId(), target.eventType());
            throw new AppException(ErrorType.NOTIFICATION_FEEDBACK_DETECTION_NOT_FOUND);
        }
        return anomalyDetectionManager
                .findDetectionId(target.wardMemberKey(), target.detectionType(), target.recordedAt())
                .orElseThrow(() -> {
                    log.warn("[알림 피드백 : 탐지 역조회 실패]: notificationId={} | wardMemberKey={} | detectionType={} | recordedAt={}",
                            target.notificationId(), target.wardMemberKey(), target.detectionType(), target.recordedAt());
                    return new AppException(ErrorType.NOTIFICATION_FEEDBACK_DETECTION_NOT_FOUND);
                });
    }
}
