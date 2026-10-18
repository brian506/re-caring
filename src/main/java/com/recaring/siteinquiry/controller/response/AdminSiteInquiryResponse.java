package com.recaring.siteinquiry.controller.response;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryCategory;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record AdminSiteInquiryResponse(
        String inquiryKey,
        SiteInquiryCategory category,
        String title,
        String content,
        String email,
        String phone,
        boolean isSecret,
        SiteInquiryStatus status,
        Instant createdAt,
        String answer,
        Instant answeredAt
) {
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Seoul");

    public static AdminSiteInquiryResponse from(SiteInquiryInfo inquiry) {
        return new AdminSiteInquiryResponse(
                inquiry.inquiryKey(),
                inquiry.category(),
                inquiry.title(),
                inquiry.content(),
                inquiry.email(),
                inquiry.phone(),
                inquiry.secret(),
                inquiry.status(),
                toInstant(inquiry.createdAt()),
                inquiry.answer(),
                toInstant(inquiry.answeredAt())
        );
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(STORAGE_ZONE).toInstant();
    }
}
