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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "fcm_device_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmDeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_device_token_id")
    private Long id;

    @Column(name = "member_key", nullable = false)
    private String memberKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private NotificationRecipientType recipientType;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20)
    private FcmDevicePlatform platform;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "deactivation_reason", length = 100)
    private String deactivationReason;

    @Builder
    public FcmDeviceToken(
            String memberKey,
            NotificationRecipientType recipientType,
            String token,
            FcmDevicePlatform platform
    ) {
        this.memberKey = memberKey;
        this.recipientType = recipientType;
        this.token = token;
        this.platform = platform;
        this.active = true;
    }

    public void assignTo(String memberKey, NotificationRecipientType recipientType, FcmDevicePlatform platform) {
        this.memberKey = memberKey;
        this.recipientType = recipientType;
        this.platform = platform;
        this.active = true;
        this.deactivatedAt = null;
        this.deactivationReason = null;
        update();
    }

    public void touchLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
        update();
    }

    public void deactivate(String reason) {
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
        this.deactivationReason = reason;
        update();
    }
}
