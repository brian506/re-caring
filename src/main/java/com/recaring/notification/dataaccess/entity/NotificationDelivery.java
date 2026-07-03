package com.recaring.notification.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notification_deliveries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_delivery_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "recipient_member_key", nullable = false)
    private String recipientMemberKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private NotificationRecipientType recipientType;

    @Column(name = "fcm_device_token_id")
    private Long fcmDeviceTokenId;

    @Column(name = "token_snapshot", nullable = false, length = 512)
    private String tokenSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationDeliveryStatus status;

    @Column(name = "fcm_message_id")
    private String fcmMessageId;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "retryable", nullable = false)
    private boolean retryable;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    private NotificationDelivery(Notification notification, FcmDeviceToken token) {
        this.notification = notification;
        this.recipientMemberKey = token.getMemberKey();
        this.recipientType = token.getRecipientType();
        this.fcmDeviceTokenId = token.getId();
        this.tokenSnapshot = token.getToken();
        this.status = NotificationDeliveryStatus.REQUESTED;
        this.attemptCount = 0;
        this.retryable = false;
    }

    public static NotificationDelivery requested(Notification notification, FcmDeviceToken token) {
        return new NotificationDelivery(notification, token);
    }

    public void markSent(String fcmMessageId, int attemptCount) {
        this.status = NotificationDeliveryStatus.SENT;
        this.fcmMessageId = fcmMessageId;
        this.failureCode = null;
        this.failureReason = null;
        this.attemptCount = attemptCount;
        this.retryable = false;
        this.nextRetryAt = null;
        this.sentAt = LocalDateTime.now();
        update();
    }

    public void markFailed(
            String failureCode,
            String failureReason,
            int attemptCount,
            boolean retryable,
            LocalDateTime nextRetryAt
    ) {
        this.status = NotificationDeliveryStatus.FAILED;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.attemptCount = attemptCount;
        this.retryable = retryable;
        this.nextRetryAt = nextRetryAt;
        this.sentAt = null;
        update();
    }
}
