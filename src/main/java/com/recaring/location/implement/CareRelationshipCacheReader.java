package com.recaring.location.implement;

import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareRelationshipCacheReader {

    private final CareRelationshipRepository careRelationshipRepository;

    @Cacheable(value = "careRelationship", key = "#wardKey + ':' + #caregiverKey + ':CAREGIVER'")
    public boolean hasCaregiverAccess(String wardKey, String caregiverKey) {
        return careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.GUARDIAN)
                || careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.MANAGER);
    }

    @Cacheable(value = "careRelationship", key = "#wardKey + ':' + #caregiverKey + ':GUARDIAN'")
    public boolean hasGuardianAccess(String wardKey, String caregiverKey) {
        return careRelationshipRepository.existsByWardKeyAndCaregiverKeyAndCareRole(wardKey, caregiverKey, CareRole.GUARDIAN);
    }
}
