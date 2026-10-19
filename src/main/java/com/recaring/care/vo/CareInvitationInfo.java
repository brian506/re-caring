package com.recaring.care.vo;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;

public record CareInvitationInfo(
        String requestKey,
        String requesterMemberKey,
        String targetMemberKey,
        String wardMemberKey,
        String caregiverKey,
        CareRole careRole,
        CarePartyRole targetRole,
        CareInvitationStatus status
) {
    public static CareInvitationInfo from(CareInvitation entity) {
        return new CareInvitationInfo(
                entity.getRequestKey(),
                entity.getRequesterMemberKey(),
                entity.getTargetMemberKey(),
                entity.getWardMemberKey(),
                entity.getCaregiverKey(),
                entity.getCareRole(),
                entity.targetRole(),
                entity.getStatus()
        );
    }

    public boolean isPending() {
        return status == CareInvitationStatus.PENDING;
    }
}
