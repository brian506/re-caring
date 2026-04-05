package com.recaring.care.dataaccess.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CareInvitationStatus {
    PENDING("대기"),
    ACCEPTED("수락"),
    REJECTED("거절");

    private final String description;
}
