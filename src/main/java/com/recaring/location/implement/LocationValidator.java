package com.recaring.location.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationValidator {

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberReader memberReader;

    public void validateWardRole(String wardMemberKey) {
        var member = memberReader.findByMemberKey(wardMemberKey);
        if (member.getRole() != MemberRole.WARD) {
            throw new AppException(ErrorType.NOT_WARD_MEMBER);
        }
    }

    public void validateCaregiverAccess(String caregiverKey, String wardKey) {
        boolean hasRelationship =
                careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.GUARDIAN) ||
                careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.MANAGER);
        if (!hasRelationship) {
            throw new AppException(ErrorType.NOT_CARE_RELATED_WARD);
        }
    }

    public void validateGuardianAccess(String caregiverKey, String wardKey) {
        boolean isGuardian = careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(
                wardKey,
                caregiverKey,
                CareRole.GUARDIAN
        );
        if (!isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }
    }
}
