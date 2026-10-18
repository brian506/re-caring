package com.recaring.siteinquiry.vo;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryCategory;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;

import java.time.LocalDateTime;

public record SiteInquiryInfo(
        String inquiryKey,
        SiteInquiryCategory category,
        String title,
        String content,
        String email,
        String phone,
        boolean secret,
        SiteInquiryStatus status,
        LocalDateTime createdAt,
        String answer,
        LocalDateTime answeredAt
) {
    public static SiteInquiryInfo from(SiteInquiry entity) {
        return new SiteInquiryInfo(
                entity.getInquiryKey(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getContent(),
                entity.getEmail(),
                entity.getPhone(),
                entity.isSecret(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getAnswer(),
                entity.getAnsweredAt()
        );
    }
}
