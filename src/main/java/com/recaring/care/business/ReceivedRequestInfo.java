package com.recaring.care.business;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.member.dataaccess.entity.Member;

import java.time.LocalDateTime;

public record ReceivedRequestInfo(
        String requestKey,
        String requesterKey,
        String requesterName,
        String wardKey,
        String wardName,
        CareRole careRole,
        CareInvitationStatus status,
        LocalDateTime createdAt
) {
    public static ReceivedRequestInfo of(CareInvitation invitation, Member requester, Member ward) {
        return new ReceivedRequestInfo(
                invitation.getRequestKey(),
                requester.getMemberKey(),
                requester.getName(),
                ward.getMemberKey(),
                ward.getName(),
                invitation.getCareRole(),
                invitation.getStatus(),
                invitation.getCreatedAt()
        );
    }
}
