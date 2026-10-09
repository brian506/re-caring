package com.recaring.inquiry.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryCategory {
    LOCATION_SAFE_ZONE("위치·안심존"),
    ALERT_EMERGENCY("알림·응급"),
    CONNECTION_ACCOUNT("연결·계정"),
    ERROR_OTHER("오류·기타");

    private final String description;
}
