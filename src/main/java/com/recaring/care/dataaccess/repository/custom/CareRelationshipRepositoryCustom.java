package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;

import java.util.List;
import java.util.Optional;

public interface CareRelationshipRepositoryCustom {

    List<CareRelationship> findAllByWardMemberKey(String wardMemberKey);

    List<CareRelationship> findAllByCaregiverMemberKey(String caregiverMemberKey);

    boolean existsCareRelationship(String wardKey, String caregiverKey, CareRole careRole);

    boolean existsCareRelationship(String wardKey, String caregiverKey);

    Optional<CareRelationship> findCareRelationship(String wardKey, String caregiverKey);

    void deleteAllByMemberKey(String memberKey);
}
