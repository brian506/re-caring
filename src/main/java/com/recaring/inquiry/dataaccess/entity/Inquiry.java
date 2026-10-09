package com.recaring.inquiry.dataaccess.entity;

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

// TODO: CREATE INDEX idx_inquiries_member_key_id ON inquiries(member_key, inquiry_id DESC);
// TODO: CREATE INDEX idx_inquiries_status_id ON inquiries(status, inquiry_id);
@Getter
@Entity
@Table(name = "inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long id;

    @Column(name = "inquiry_key", nullable = false, unique = true)
    private String inquiryKey;

    @Column(name = "member_key", nullable = false)
    private String memberKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private InquiryCategory category;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "answer", length = 2000)
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    public Inquiry(String memberKey, InquiryCategory category, String title, String content) {
        this.inquiryKey = UUID.randomUUID().toString();
        this.memberKey = memberKey;
        this.category = category;
        this.title = title;
        this.content = content;
        this.status = InquiryStatus.WAITING;
    }

    public void answer(String answer, LocalDateTime answeredAt) {
        this.answer = answer;
        this.answeredAt = answeredAt;
        this.status = InquiryStatus.ANSWERED;
        update();
    }
}
