package com.recaring.care.controller.response;

import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.vo.CarePartyRole;
import com.recaring.care.vo.SentRequestInfo;

import java.time.LocalDateTime;

public record SentCareRequestResponse(
        String requestKey,
        String phoneNumber,
        CarePartyRole targetRole,
        CareInvitationStatus status,
        LocalDateTime createdAt,
        String wardKey,
        String wardName
) {
    public static SentCareRequestResponse from(SentRequestInfo info) {
        return new SentCareRequestResponse(
                info.requestKey(),
                info.phoneNumber(),
                info.targetRole(),
                info.status(),
                info.createdAt(),
                info.wardKey(),
                info.wardName()
        );
    }
}
