package com.recaring.care.dataaccess.repository;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.custom.CareRelationshipRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareRelationshipRepository extends JpaRepository<CareRelationship, Long>,
        CareRelationshipRepositoryCustom {
}
