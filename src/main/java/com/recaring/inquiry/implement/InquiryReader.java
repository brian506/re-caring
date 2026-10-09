package com.recaring.inquiry.implement;

import com.recaring.inquiry.dataaccess.entity.InquiryStatus;
import com.recaring.inquiry.dataaccess.repository.InquiryRepository;
import com.recaring.inquiry.vo.InquiryInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryReader {

    private final InquiryRepository inquiryRepository;

    public List<InquiryInfo> findByMemberKey(String memberKey) {
        return inquiryRepository.findByMemberKeyOrderByIdDesc(memberKey).stream()
                .map(InquiryInfo::from)
                .toList();
    }

    public List<InquiryInfo> findAll(InquiryStatus status) {
        if (status == null) {
            return inquiryRepository.findAllByOrderByIdDesc().stream()
                    .map(InquiryInfo::from)
                    .toList();
        }
        return inquiryRepository.findByStatusOrderByIdAsc(status).stream()
                .map(InquiryInfo::from)
                .toList();
    }
}
