package com.recaring.notification.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "notification_feedbacks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_feedbacks_notification",
                columnNames = "notification_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_feedback_id")
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "anomaly_detection_id", nullable = false)
    private Long anomalyDetectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackAccuracy accuracy;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FeedbackReason reason;

    @Column(length = 100)
    private String comment;

    @Builder
    public NotificationFeedback(Long notificationId, Long anomalyDetectionId,
                                FeedbackAccuracy accuracy, FeedbackReason reason, String comment) {
        this.notificationId = notificationId;
        this.anomalyDetectionId = anomalyDetectionId;
        this.accuracy = accuracy;
        this.reason = reason;
        this.comment = comment;
    }
}
