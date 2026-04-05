package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.List;

import static com.recaring.care.dataaccess.entity.QCareRelationship.careRelationship;

public class CareRelationshipRepositoryCustomImpl extends QuerydslRepositorySupport
        implements CareRelationshipRepositoryCustom {

    protected CareRelationshipRepositoryCustomImpl() {
        super(CareRelationship.class);
    }

    @Override
    public List<CareRelationship> findAllByWardMemberKey(String wardMemberKey) {
        return selectFrom(careRelationship)
                .where(careRelationship.wardKey.eq(wardMemberKey))
                .fetch();
    }

    @Override
    public List<CareRelationship> findAllByCaregiverKey(String caregiverKey) {
        return selectFrom(careRelationship)
                .where(careRelationship.caregiverKey.eq(caregiverKey))
                .fetch();
    }

    @Override
    public boolean existsByWardKeyAndCaregiverKeyAndCareRole(String wardKey, String caregiverKey, CareRole careRole) {
        Integer result = selectOne()
                .from(careRelationship)
                .where(
                        careRelationship.wardKey.eq(wardKey),
                        careRelationship.caregiverKey.eq(caregiverKey),
                        careRelationship.careRole.eq(careRole)
                )
                .fetchFirst();
        return result != null;
    }
}
