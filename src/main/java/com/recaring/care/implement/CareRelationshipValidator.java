package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CareRelationshipValidator {

    private static final int MAX_WARD_COUNT = 3;

    private final CareRelationshipRepository careRelationshipRepository;

    public void validateCanAdd(String caregiverMemberKey, String newWardMemberKey) {
        List<CareRelationship> careRelationships = careRelationshipRepository.findAllByCaregiverKey(caregiverMemberKey);
        if(careRelationships.isEmpty()) return;

        if (careRelationships.size() >= MAX_WARD_COUNT) {
            throw new AppException(ErrorType.CARE_CAREGIVER_LIMIT_EXCEEDED);
        }

        boolean isDuplicated = careRelationships.stream()
                .anyMatch(relation -> relation.getWardMemberKey().equals(newWardMemberKey));
        if (isDuplicated) {
            throw new AppException(ErrorType.ALREADY_CARE_RELATIONSHIP);
        }
    }


}
