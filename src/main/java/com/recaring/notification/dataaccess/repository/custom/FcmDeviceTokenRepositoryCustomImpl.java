package com.recaring.notification.dataaccess.repository.custom;

import com.querydsl.core.BooleanBuilder;
import com.recaring.care.vo.CarePartyRole;
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
    public List<FcmDeviceToken> findGuardianTokens(Collection<String> memberKeys) {
        if (memberKeys == null || memberKeys.isEmpty()) {
            return List.of();
        }
        return selectFrom(fcmDeviceToken)
                .where(fcmDeviceToken.memberKey.in(memberKeys),
                        fcmDeviceToken.careRole.eq(CarePartyRole.GUARDIAN))
                .fetch();
    }
}
