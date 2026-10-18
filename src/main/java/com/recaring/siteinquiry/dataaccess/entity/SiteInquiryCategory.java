package com.recaring.siteinquiry.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SiteInquiryCategory {
    FEATURE("기능 문의"),
    USAGE("이용 문의"),
    ACCOUNT_DELETION("계정 삭제(탈퇴) 요청"),
    OTHER("기타 문의");

    private final String description;
}
