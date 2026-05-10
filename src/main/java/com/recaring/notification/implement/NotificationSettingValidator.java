package com.recaring.notification.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
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
    private final CareRelationshipRepository careRelationshipRepository;

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
        boolean hasRelationship =
                careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.GUARDIAN) ||
                        careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.MANAGER);
        if (!hasRelationship) {
            throw new AppException(ErrorType.NOT_CARE_RELATED_WARD);
        }
    }
}
