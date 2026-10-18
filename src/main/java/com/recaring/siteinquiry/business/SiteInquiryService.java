package com.recaring.siteinquiry.business;

import com.recaring.inquiry.vo.InquiryAnswer;
import com.recaring.siteinquiry.business.command.SubmitSiteInquiryCommand;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.siteinquiry.implement.SiteInquiryAuthenticator;
import com.recaring.siteinquiry.implement.SiteInquiryManager;
import com.recaring.siteinquiry.implement.SiteInquiryOperatorNotifier;
import com.recaring.siteinquiry.implement.SiteInquiryRateLimitValidator;
import com.recaring.siteinquiry.implement.SiteInquiryReader;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteInquiryService {

    private final SiteInquiryManager siteInquiryManager;
    private final SiteInquiryReader siteInquiryReader;
    private final SiteInquiryAuthenticator siteInquiryAuthenticator;
    private final SiteInquiryRateLimitValidator siteInquiryRateLimitValidator;
    private final SiteInquiryOperatorNotifier siteInquiryOperatorNotifier;

    public SiteInquiryInfo submit(SubmitSiteInquiryCommand command, String clientIp) {
        siteInquiryRateLimitValidator.validateSubmit(clientIp);
        String passwordHash = siteInquiryAuthenticator.encode(command.password());
        SiteInquiryInfo inquiry = siteInquiryManager.register(command, passwordHash);
        siteInquiryOperatorNotifier.notifySubmitted(inquiry);
        return inquiry;
    }

    public List<SiteInquiryInfo> getInquiries(SiteInquiryStatus status) {
        return siteInquiryReader.findAll(status);
    }

    public SiteInquiryInfo view(String inquiryKey, String rawPassword) {
        siteInquiryAuthenticator.authenticate(inquiryKey, rawPassword);
        return siteInquiryReader.getByKey(inquiryKey);
    }

    public SiteInquiryInfo answer(String inquiryKey, InquiryAnswer answer) {
        return siteInquiryManager.answer(inquiryKey, answer);
    }

    public SiteInquiryInfo clearAnswer(String inquiryKey) {
        return siteInquiryManager.clearAnswer(inquiryKey);
    }

    public void delete(String inquiryKey) {
        siteInquiryManager.delete(inquiryKey);
    }
}
