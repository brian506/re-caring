package com.recaring.inquiry.implement;

import com.recaring.inquiry.dataaccess.entity.Inquiry;
import com.recaring.inquiry.dataaccess.entity.InquiryCategory;
import com.recaring.inquiry.dataaccess.repository.InquiryRepository;
import com.recaring.inquiry.event.InquiryAnsweredEvent;
import com.recaring.inquiry.vo.InquiryAnswer;
import com.recaring.inquiry.vo.InquiryContent;
import com.recaring.inquiry.vo.InquiryInfo;
import com.recaring.inquiry.vo.InquiryTitle;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InquiryManager {

    private final InquiryRepository inquiryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InquiryInfo register(String memberKey, InquiryCategory category, InquiryTitle title, InquiryContent content) {
        Inquiry saved = inquiryRepository.save(Inquiry.builder()
                .memberKey(memberKey)
                .category(category)
                .title(title.value())
                .content(content.value())
                .build());
        return InquiryInfo.from(saved);
    }

    @Transactional
    public InquiryInfo answer(String inquiryKey, InquiryAnswer answer) {
        Inquiry inquiry = inquiryRepository.findByInquiryKey(inquiryKey)
                .orElseThrow(() -> new AppException(ErrorType.INQUIRY_NOT_FOUND));
        inquiry.answer(answer.value(), LocalDateTime.now());
        eventPublisher.publishEvent(new InquiryAnsweredEvent(
                inquiry.getInquiryKey(), inquiry.getMemberKey(), inquiry.getTitle()));
        return InquiryInfo.from(inquiry);
    }

    @Transactional
    public void deleteByMemberKey(String memberKey) {
        inquiryRepository.deleteByMemberKey(memberKey);
    }
}
