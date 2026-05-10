package com.recaring.auth.dataaccess.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// TODO: CREATE INDEX idx_refresh_token_token ON refresh_token(token);
// TODO: CREATE INDEX idx_refresh_token_member_key ON refresh_token(member_key);
@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(name = "member_key", nullable = false, length = 36)
    private String memberKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RefreshToken(String memberKey, String token, LocalDateTime expiredAt) {
        this.memberKey = memberKey;
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public static RefreshToken of(String memberKey, String token, long expirationMs) {
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);
        return new RefreshToken(memberKey, token, expiredAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }
}
