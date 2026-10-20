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
            throw new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP);
        }
    }

    public void validateHistoryViewAccess(String requesterKey, String wardKey) {
        if (wardKey.equals(requesterKey)) {
            return;
        }
        validateCaregiverAccess(requesterKey, wardKey);
    }
}
