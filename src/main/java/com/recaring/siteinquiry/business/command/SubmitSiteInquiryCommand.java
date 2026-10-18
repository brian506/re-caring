package com.recaring.siteinquiry.business.command;

import com.recaring.inquiry.vo.InquiryContent;
import com.recaring.inquiry.vo.InquiryTitle;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryCategory;
import com.recaring.siteinquiry.vo.SiteInquiryPassword;

public record SubmitSiteInquiryCommand(
        SiteInquiryCategory category,
        InquiryTitle title,
        InquiryContent content,
        String email,
        String phone,
        SiteInquiryPassword password
) {
    public boolean secret() {
        return password != null;
    }
}
