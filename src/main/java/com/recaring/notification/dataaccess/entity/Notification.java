package com.recaring.notification.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "notification_key", nullable = false, unique = true)
    private String notificationKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Lob
    @Column(name = "data_payload")
    private String dataPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Builder
    public Notification(String eventType, String title, String body, String dataPayload) {
        this.notificationKey = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.title = title;
        this.body = body;
        this.dataPayload = dataPayload;
        this.status = NotificationStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.failureCode = null;
        this.failureReason = null;
        update();
    }

    public void markPartial(String failureCode, String failureReason) {
        this.status = NotificationStatus.PARTIAL;
        this.sentAt = LocalDateTime.now();
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        update();
    }

    public void markFailed(String failureCode, String failureReason) {
        this.status = NotificationStatus.FAILED;
        this.sentAt = LocalDateTime.now();
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        update();
    }
}
