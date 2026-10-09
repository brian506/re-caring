package com.recaring.inquiry.controller.response;

import com.recaring.inquiry.dataaccess.entity.InquiryCategory;
import com.recaring.inquiry.dataaccess.entity.InquiryStatus;
import com.recaring.inquiry.vo.InquiryInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record AdminInquiryResponse(
        String inquiryKey,
        String memberKey,
        InquiryCategory category,
        String title,
        String content,
        InquiryStatus status,
        Instant createdAt,
        String answer,
        Instant answeredAt
) {
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Seoul");

    public static AdminInquiryResponse from(InquiryInfo inquiry) {
        return new AdminInquiryResponse(
                inquiry.inquiryKey(),
                inquiry.memberKey(),
                inquiry.category(),
                inquiry.title(),
                inquiry.content(),
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
