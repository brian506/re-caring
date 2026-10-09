package com.recaring.inquiry.vo;

import com.recaring.inquiry.dataaccess.entity.Inquiry;
import com.recaring.inquiry.dataaccess.entity.InquiryCategory;
import com.recaring.inquiry.dataaccess.entity.InquiryStatus;

import java.time.LocalDateTime;

public record InquiryInfo(
        String inquiryKey,
        String memberKey,
        InquiryCategory category,
        String title,
        String content,
        InquiryStatus status,
        LocalDateTime createdAt,
        String answer,
        LocalDateTime answeredAt
) {
    public static InquiryInfo from(Inquiry entity) {
        return new InquiryInfo(
                entity.getInquiryKey(),
                entity.getMemberKey(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getContent(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getAnswer(),
                entity.getAnsweredAt()
        );
    }
}
