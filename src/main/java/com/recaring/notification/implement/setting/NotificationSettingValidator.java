package com.recaring.notification.implement.setting;

import com.recaring.care.implement.CareRelationshipReader;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSettingValidator {

    private final MemberReader memberReader;
    private final CareRelationshipReader careRelationshipReader;

    public void validateSettingAccess(String requesterKey, String wardKey) {
        Member requester = memberReader.findByMemberKey(requesterKey);
        Member ward = memberReader.findByMemberKey(wardKey);
        if (ward.getRole() != MemberRole.WARD) {
            throw new AppException(ErrorType.NOT_WARD_MEMBER);
        }

        if (requester.getRole() == MemberRole.WARD) {
            validateSelfAccess(requesterKey, wardKey);
            return;
        }

        validateCaregiverAccess(requesterKey, wardKey);
    }

    private void validateSelfAccess(String requesterKey, String wardKey) {
        if (!requesterKey.equals(wardKey)) {
            throw new AppException(ErrorType.NOT_CARE_RELATED_WARD);
        }
    }

    private void validateCaregiverAccess(String caregiverKey, String wardKey) {
        boolean hasRelationship = careRelationshipReader.exists(wardKey, caregiverKey);
        if (!hasRelationship) {
            throw new AppException(ErrorType.NOT_CARE_RELATED_WARD);
        }
    }
}
