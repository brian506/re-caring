package com.recaring.notification.dataaccess.repository.custom;

import com.querydsl.core.BooleanBuilder;
import com.recaring.care.dataaccess.entity.CarePartyRole;
import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.Collection;
import java.util.List;

import static com.recaring.notification.dataaccess.entity.QFcmDeviceToken.fcmDeviceToken;

public class FcmDeviceTokenRepositoryCustomImpl extends QuerydslRepositorySupport
        implements FcmDeviceTokenRepositoryCustom {

    protected FcmDeviceTokenRepositoryCustomImpl() {
        super(FcmDeviceToken.class);
    }

    @Override
    public void deleteByMemberKey(String memberKey) {
        delete(fcmDeviceToken)
                .where(fcmDeviceToken.memberKey.eq(memberKey))
                .execute();
    }

    @Override
    public void deleteByToken(String token) {
        delete(fcmDeviceToken)
                .where(fcmDeviceToken.token.eq(token))
                .execute();
    }

    @Override
    public void deleteByTokenIn(Collection<String> tokens) {
        delete(fcmDeviceToken)
                .where(fcmDeviceToken.token.in(tokens))
                .execute();
    }

    @Override
    public List<FcmDeviceToken> findTokensByCareRoles(
            Collection<String> guardianMemberKeys,
            Collection<String> managerMemberKeys
    ) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (guardianMemberKeys != null && !guardianMemberKeys.isEmpty()) {
            predicate.or(fcmDeviceToken.memberKey.in(guardianMemberKeys)
                    .and(fcmDeviceToken.careRole.eq(CarePartyRole.GUARDIAN)));
        }
        if (managerMemberKeys != null && !managerMemberKeys.isEmpty()) {
            predicate.or(fcmDeviceToken.memberKey.in(managerMemberKeys)
                    .and(fcmDeviceToken.careRole.eq(CarePartyRole.MANAGER)));
        }
        return selectFrom(fcmDeviceToken).where(predicate).fetch();
    }
}
