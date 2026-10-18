package com.recaring.siteinquiry.dataaccess.entity;

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
import java.util.UUID;

// TODO: CREATE INDEX idx_site_inquiries_status_id ON site_inquiries(status, site_inquiry_id);
@Getter
@Entity
@Table(name = "site_inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteInquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_inquiry_id")
    private Long id;

    @Column(name = "inquiry_key", nullable = false, unique = true)
    private String inquiryKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private SiteInquiryCategory category;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_secret", nullable = false)
    private boolean secret;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SiteInquiryStatus status;

    @Column(name = "answer", length = 2000)
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    public SiteInquiry(SiteInquiryCategory category, String title, String content, String email, String phone,
                       boolean secret, String passwordHash) {
        this.inquiryKey = UUID.randomUUID().toString();
        this.category = category;
        this.title = title;
        this.content = content;
        this.email = email;
        this.phone = phone;
        this.secret = secret;
        this.passwordHash = passwordHash;
        this.status = SiteInquiryStatus.WAITING;
    }

    public void answer(String answer, LocalDateTime answeredAt) {
        this.answer = answer;
        this.answeredAt = answeredAt;
        this.status = SiteInquiryStatus.ANSWERED;
        update();
    }

    public void clearAnswer() {
        this.answer = null;
        this.answeredAt = null;
        this.status = SiteInquiryStatus.WAITING;
        update();
    }
}
