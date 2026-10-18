package com.recaring.siteinquiry.implement;

import com.recaring.inquiry.vo.InquiryAnswer;
import com.recaring.siteinquiry.business.command.SubmitSiteInquiryCommand;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.repository.SiteInquiryRepository;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SiteInquiryManager {

    private final SiteInquiryRepository siteInquiryRepository;

    @Transactional
    public SiteInquiryInfo register(SubmitSiteInquiryCommand command, String passwordHash) {
        SiteInquiry saved = siteInquiryRepository.save(SiteInquiry.builder()
                .category(command.category())
                .title(command.title().value())
                .content(command.content().value())
                .email(command.email())
                .phone(command.phone())
                .secret(command.secret())
                .passwordHash(passwordHash)
                .build());
        return SiteInquiryInfo.from(saved);
    }

    @Transactional
    public SiteInquiryInfo answer(String inquiryKey, InquiryAnswer answer) {
        SiteInquiry inquiry = get(inquiryKey);
        inquiry.answer(answer.value(), LocalDateTime.now());
        return SiteInquiryInfo.from(inquiry);
    }

    @Transactional
    public SiteInquiryInfo clearAnswer(String inquiryKey) {
        SiteInquiry inquiry = get(inquiryKey);
        inquiry.clearAnswer();
        return SiteInquiryInfo.from(inquiry);
    }

    @Transactional
    public void delete(String inquiryKey) {
        siteInquiryRepository.delete(get(inquiryKey));
    }

    private SiteInquiry get(String inquiryKey) {
        return siteInquiryRepository.findByInquiryKey(inquiryKey)
                .orElseThrow(() -> new AppException(ErrorType.SITE_INQUIRY_NOT_FOUND));
    }
}
