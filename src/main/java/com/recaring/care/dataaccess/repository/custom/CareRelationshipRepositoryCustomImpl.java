package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.vo.CareRole;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.List;
import java.util.Optional;

import static com.recaring.care.dataaccess.entity.QCareRelationship.careRelationship;

public class CareRelationshipRepositoryCustomImpl extends QuerydslRepositorySupport
        implements CareRelationshipRepositoryCustom {

    protected CareRelationshipRepositoryCustomImpl() {
        super(CareRelationship.class);
    }

    @Override
    public List<CareRelationship> findAllByWardMemberKey(String wardMemberKey) {
        return selectFrom(careRelationship)
                .where(careRelationship.wardMemberKey.eq(wardMemberKey))
                .fetch();
    }

    @Override
    public List<CareRelationship> findAllByCaregiverMemberKey(String caregiverMemberKey) {
        return selectFrom(careRelationship)
                .where(careRelationship.caregiverMemberKey.eq(caregiverMemberKey))
                .fetch();
    }

    @Override
    public boolean existsCareRelationship(String wardKey, String caregiverKey) {
        Integer result = selectOne()
                .from(careRelationship)
                .where(
                        careRelationship.wardMemberKey.eq(wardKey),
                        careRelationship.caregiverMemberKey.eq(caregiverKey)
                )
                .fetchFirst();
        return result != null;
    }

    @Override
    public Optional<CareRelationship> findCareRelationship(String wardKey, String caregiverKey) {
        CareRelationship result = selectFrom(careRelationship)
                .where(
                        careRelationship.wardMemberKey.eq(wardKey),
                        careRelationship.caregiverMemberKey.eq(caregiverKey)
                )
                .fetchFirst();
        return Optional.ofNullable(result);
    }

    @Override
    public void deleteAllByWardMemberKey(String wardMemberKey) {
        delete(careRelationship)
                .where(careRelationship.wardMemberKey.eq(wardMemberKey))
                .execute();
    }

    @Override
    public void deleteAllByMemberKey(String memberKey) {
        delete(careRelationship)
                .where(
                        careRelationship.wardMemberKey.eq(memberKey)
                                .or(careRelationship.caregiverMemberKey.eq(memberKey))
                )
                .execute();
    }
}
