package com.recaring.inquiry.implement;

import com.recaring.inquiry.vo.InquiryInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InquiryOperatorNotifier {

    public void notifySubmitted(InquiryInfo inquiry) {
        log.info("[1:1 문의 : 신규 접수]: inquiryKey={} | memberKey={} | category={} | createdAt={} | title={} | content={}",
                inquiry.inquiryKey(), inquiry.memberKey(), inquiry.category(), inquiry.createdAt(),
                inquiry.title(), inquiry.content());
    }
}
