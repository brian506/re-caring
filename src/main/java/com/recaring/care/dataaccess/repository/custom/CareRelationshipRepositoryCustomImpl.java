package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.Collection;
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
    public boolean existsCareRelationship(String wardKey, String caregiverKey, CareRole careRole) {
        Integer result = selectOne()
                .from(careRelationship)
                .where(
                        careRelationship.wardMemberKey.eq(wardKey),
                        careRelationship.caregiverMemberKey.eq(caregiverKey),
                        careRelationship.careRole.eq(careRole)
                )
                .fetchFirst();
        return result != null;
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
    public boolean existsCareRelationshipInRoles(String wardKey, String caregiverKey, Collection<CareRole> careRoles) {
        Integer result = selectOne()
                .from(careRelationship)
                .where(
                        careRelationship.wardMemberKey.eq(wardKey),
                        careRelationship.caregiverMemberKey.eq(caregiverKey),
                        careRelationship.careRole.in(careRoles)
                )
                .fetchFirst();
        return result != null;
    }

    @Override
    public boolean existsCareRelationshipWithRole(String wardKey, CareRole careRole) {
        Integer result = selectOne()
                .from(careRelationship)
                .where(
                        careRelationship.wardMemberKey.eq(wardKey),
                        careRelationship.careRole.eq(careRole)
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
