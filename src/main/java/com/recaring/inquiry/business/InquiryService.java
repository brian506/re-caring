package com.recaring.inquiry.business;

import com.recaring.inquiry.business.command.SubmitInquiryCommand;
import com.recaring.inquiry.dataaccess.entity.InquiryStatus;
import com.recaring.inquiry.implement.InquiryManager;
import com.recaring.inquiry.implement.InquiryOperatorNotifier;
import com.recaring.inquiry.implement.InquiryReader;
import com.recaring.inquiry.vo.InquiryAnswer;
import com.recaring.inquiry.vo.InquiryInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryManager inquiryManager;
    private final InquiryReader inquiryReader;
    private final InquiryOperatorNotifier inquiryOperatorNotifier;

    public InquiryInfo submit(String memberKey, SubmitInquiryCommand command) {
        InquiryInfo inquiry = inquiryManager.register(memberKey, command.category(), command.title(), command.content());
        inquiryOperatorNotifier.notifySubmitted(inquiry);
        return inquiry;
    }

    public List<InquiryInfo> getMyInquiries(String memberKey) {
        return inquiryReader.findByMemberKey(memberKey);
    }

    public List<InquiryInfo> getInquiries(InquiryStatus status) {
        return inquiryReader.findAll(status);
    }

    public InquiryInfo answer(String inquiryKey, InquiryAnswer answer) {
        return inquiryManager.answer(inquiryKey, answer);
    }
}
