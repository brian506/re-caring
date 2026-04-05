package com.recaring.care.business;

import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRole;

import java.time.LocalDateTime;

public record ReceivedRequestInfo(
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
}
