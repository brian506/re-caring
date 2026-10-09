package com.recaring.care.vo;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

public record NewCareInvitation(
        String requesterMemberKey,
        String targetMemberKey,
        String wardMemberKey,
        CareRole careRole
) {
    public NewCareInvitation {
        if (requesterMemberKey == null || requesterMemberKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
        if (targetMemberKey == null || targetMemberKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
        if (wardMemberKey == null || wardMemberKey.isBlank()) {
            throw new AppException(ErrorType.INVALID_MEMBER_KEY);
        }
        if (careRole == null) {
            throw new AppException(ErrorType.FAILED_AUTH);
        }
    }

    public static NewCareInvitation ofWardRequest(String requesterMemberKey, Ward ward) {
        return new NewCareInvitation(
                requesterMemberKey,
                ward.memberKey(),
                ward.memberKey(),
                CareRole.GUARDIAN
        );
    }


    public static NewCareInvitation ofCaregiverRequest(String requesterKey, Caregiver caregiver, String wardMemberKey, CareRole careRole) {
        return new NewCareInvitation(
                requesterKey,
                caregiver.memberKey(),
                wardMemberKey,
                careRole
        );
    }
}
