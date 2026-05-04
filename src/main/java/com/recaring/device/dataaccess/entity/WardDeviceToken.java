package com.recaring.device.dataaccess.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "ward_device_tokens")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WardDeviceToken {

    // TODO: CREATE INDEX idx_ward_device_tokens_token ON ward_device_tokens (token);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String wardKey;

    @Column(nullable = false, unique = true)
    private String token;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    @Builder
    public WardDeviceToken(String wardKey) {
        this.wardKey = wardKey;
        this.token = UUID.randomUUID().toString();
    }

    public void reissue() {
        this.token = UUID.randomUUID().toString();
    }
}
