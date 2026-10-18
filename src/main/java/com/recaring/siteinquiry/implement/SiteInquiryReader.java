package com.recaring.siteinquiry.implement;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import com.recaring.siteinquiry.dataaccess.repository.SiteInquiryRepository;
import com.recaring.siteinquiry.vo.SiteInquiryInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteInquiryReader {

    private final SiteInquiryRepository siteInquiryRepository;

    public SiteInquiryInfo getByKey(String inquiryKey) {
        return siteInquiryRepository.findByInquiryKey(inquiryKey)
                .map(SiteInquiryInfo::from)
                .orElseThrow(() -> new AppException(ErrorType.SITE_INQUIRY_NOT_FOUND));
    }

    public List<SiteInquiryInfo> findAll(SiteInquiryStatus status) {
        if (status == null) {
            return siteInquiryRepository.findAllByOrderByIdDesc().stream()
                    .map(SiteInquiryInfo::from)
                    .toList();
        }
        return siteInquiryRepository.findByStatusOrderByIdAsc(status).stream()
                .map(SiteInquiryInfo::from)
                .toList();
    }
}
