package com.recaring.location.implement;

import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationValidator {

    private final CareRelationshipCacheReader careRelationshipCacheReader;

    public void validateCaregiverAccess(String caregiverKey, String wardKey) {
        boolean hasRelationship = careRelationshipCacheReader.hasCaregiverAccess(wardKey, caregiverKey);
        if (!hasRelationship) {
            throw new AppException(ErrorType.NOT_CARE_RELATED_WARD);
        }
    }

    public void validateGuardianAccess(String caregiverKey, String wardKey) {
        boolean isGuardian = careRelationshipCacheReader.hasGuardianAccess(wardKey, caregiverKey);
        if (!isGuardian) {
            throw new AppException(ErrorType.NOT_GUARDIAN_OF_WARD);
        }
    }
}
