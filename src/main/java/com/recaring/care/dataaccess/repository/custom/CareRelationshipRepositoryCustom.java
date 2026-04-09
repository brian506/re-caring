package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;

import java.util.List;

public interface CareRelationshipRepositoryCustom {

    List<CareRelationship> findAllByWardMemberKey(String wardMemberKey);

    List<CareRelationship> findAllByCaregiverMemberKey(String caregiverMemberKey);

    boolean existsByWardKeyAndCaregiverKeyAndCareRole(String wardKey, String caregiverKey, CareRole careRole);
}
