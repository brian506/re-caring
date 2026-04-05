package com.recaring.member.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agreement {

    @Column(nullable = false)
    private LocalDateTime termsServiceAgreedAt;

    @Column(nullable = false)
    private LocalDateTime termsPrivacyAgreedAt;

    @Column(nullable = false)
    private LocalDateTime termsLocationAgreedAt;

    private Agreement(LocalDateTime termsServiceAgreedAt, LocalDateTime termsPrivacyAgreedAt, LocalDateTime termsLocationAgreedAt) {
        this.termsServiceAgreedAt = termsServiceAgreedAt;
        this.termsPrivacyAgreedAt = termsPrivacyAgreedAt;
        this.termsLocationAgreedAt = termsLocationAgreedAt;
    }

    public static Agreement allAgreedNow() {
        LocalDateTime now = LocalDateTime.now();
        return new Agreement(now, now, now);
    }
}
