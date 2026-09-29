package com.recaring.care.dataaccess.repository;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.custom.CareRelationshipRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CareRelationshipRepository extends JpaRepository<CareRelationship, Long>,
        CareRelationshipRepositoryCustom {

    @Query("select distinct c.wardMemberKey from CareRelationship c")
    List<String> findAllWardMemberKeys();
}
