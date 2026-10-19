package com.recaring.care.vo;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.member.vo.MemberInfo;

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
    public static ReceivedRequestInfo of(CareInvitation invitation, MemberInfo requester, MemberInfo ward) {
        return new ReceivedRequestInfo(
                invitation.getRequestKey(),
                requester.memberKey(),
                requester.name(),
                ward.memberKey(),
                ward.name(),
                invitation.getCareRole(),
                invitation.getStatus(),
                invitation.getCreatedAt()
        );
    }
}
