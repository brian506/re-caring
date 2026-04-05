package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CareRelationshipReader {

    private final CareRelationshipRepository careRelationshipRepository;

    public List<CareRelationship> findAllByWardKey(String wardKey) {
        return careRelationshipRepository.findAllByWardMemberKey(wardKey);
    }

    public List<CareRelationship> findAllByCaregiverKey(String caregiverKey) {
        return careRelationshipRepository.findAllByCaregiverKey(caregiverKey);
    }


    public boolean isGuardianOf(String caregiverKey, String wardKey) {
        return careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.GUARDIAN);
    }

}
