package com.recaring.siteinquiry.controller.response;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryCategory;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;

import java.time.Instant;
import java.time.ZoneId;

public record SiteInquirySummaryResponse(
        String inquiryKey,
        SiteInquiryCategory category,
        String title,
        SiteInquiryStatus status,
        boolean isSecret,
        Instant createdAt
) {
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Seoul");

    public static SiteInquirySummaryResponse from(SiteInquiryInfo inquiry) {
        return new SiteInquirySummaryResponse(
                inquiry.inquiryKey(),
                inquiry.category(),
                inquiry.title(),
                inquiry.status(),
                inquiry.secret(),
                inquiry.createdAt().atZone(STORAGE_ZONE).toInstant()
        );
    }
}
