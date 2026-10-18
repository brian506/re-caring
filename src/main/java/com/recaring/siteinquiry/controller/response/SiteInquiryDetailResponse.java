package com.recaring.siteinquiry.controller.response;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryCategory;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record SiteInquiryDetailResponse(
        String inquiryKey,
        SiteInquiryCategory category,
        String title,
        String content,
        SiteInquiryStatus status,
        boolean isSecret,
        Instant createdAt,
        String answer,
        Instant answeredAt
) {
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Seoul");

    public static SiteInquiryDetailResponse from(SiteInquiryInfo inquiry) {
        return new SiteInquiryDetailResponse(
                inquiry.inquiryKey(),
                inquiry.category(),
                inquiry.title(),
                inquiry.content(),
                inquiry.status(),
                inquiry.secret(),
                toInstant(inquiry.createdAt()),
                inquiry.answer(),
                toInstant(inquiry.answeredAt())
        );
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(STORAGE_ZONE).toInstant();
    }
}
