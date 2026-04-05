package com.recaring.care.controller.response;

import com.recaring.care.business.ReceivedRequestInfo;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRole;

import java.time.LocalDateTime;

public record ReceivedCareRequestResponse(
        String requestKey,
        String requesterKey,
        String requesterName,
        String requesterPhone,
        String wardKey,
        String wardName,
        CareRole careRole,
        CareInvitationStatus status,
        LocalDateTime expiredAt,
        LocalDateTime createdAt
) {
    public static ReceivedCareRequestResponse from(ReceivedRequestInfo info) {
        return new ReceivedCareRequestResponse(
                info.requestKey(),
                info.requesterKey(),
                info.requesterName(),
                info.requesterPhone(),
                info.wardKey(),
                info.wardName(),
                info.careRole(),
                info.status(),
                info.expiredAt(),
                info.createdAt()
        );
    }
}
