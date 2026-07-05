package com.recaring.notification.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

// TODO: CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_member_key, created_at DESC);
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

    @Column(name = "recipient_member_key", nullable = false)
    private String recipientMemberKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_payload")
    private Map<String, String> dataPayload;

    @Builder
    public Notification(String recipientMemberKey, String eventType, String title, String body, Map<String, String> dataPayload) {
        this.notificationKey = UUID.randomUUID().toString();
        this.recipientMemberKey = recipientMemberKey;
        this.eventType = eventType;
        this.title = title;
        this.body = body;
        this.dataPayload = dataPayload;
    }
}
