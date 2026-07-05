package com.recaring.notification.dataaccess.entity;

import com.recaring.care.dataaccess.entity.CareRole;
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
    private CareRole careRole;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20)
    private FcmDevicePlatform platform;

    @Builder
    public FcmDeviceToken(
            String memberKey,
            CareRole careRole,
            String token,
            FcmDevicePlatform platform
    ) {
        this.memberKey = memberKey;
        this.careRole = careRole;
        this.token = token;
        this.platform = platform;
    }

    public void assignTo(String memberKey, CareRole careRole, FcmDevicePlatform platform) {
        this.memberKey = memberKey;
        this.careRole = careRole;
        this.platform = platform;
        update();
    }


}
