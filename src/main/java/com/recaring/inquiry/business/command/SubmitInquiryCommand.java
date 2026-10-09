package com.recaring.inquiry.business.command;

import com.recaring.inquiry.dataaccess.entity.InquiryCategory;
import com.recaring.inquiry.vo.InquiryContent;
import com.recaring.inquiry.vo.InquiryTitle;

public record SubmitInquiryCommand(
        InquiryCategory category,
        InquiryTitle title,
        InquiryContent content
) {
}
