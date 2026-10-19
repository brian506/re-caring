package com.recaring.care.vo;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.member.vo.MemberInfo;

import java.time.LocalDateTime;

public record SentRequestInfo(
        String requestKey,
        String phoneNumber,
        CarePartyRole targetRole,
        CareInvitationStatus status,
        LocalDateTime createdAt,
        String wardKey,
        String wardName
) {
    public static SentRequestInfo of(CareInvitation invitation, MemberInfo target, MemberInfo ward) {
        CarePartyRole targetRole = invitation.targetRole();
        boolean revealWard = targetRole != CarePartyRole.WARD || invitation.getStatus() == CareInvitationStatus.ACCEPTED;
        return new SentRequestInfo(
                invitation.getRequestKey(),
                target.phone(),
                targetRole,
                invitation.getStatus(),
                invitation.getCreatedAt(),
                revealWard ? ward.memberKey() : null,
                revealWard ? ward.name() : null
        );
    }
}
