package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.careInvitationStatus;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.List;
import java.util.Optional;

import static com.recaring.care.dataaccess.entity.QcareInvitation.careInvitation;

public class CareInvitationRepositoryCustomImpl extends QuerydslRepositorySupport
        implements CareInvitationRepositoryCustom {

    protected CareInvitationRepositoryCustomImpl() {
        super(CareInvitation.class);
    }

    @Override
    public List<CareInvitation> findReceivedPendingRequests(String targetKey) {
        return selectFrom(careInvitation)
                .where(
                        careInvitation.targetKey.eq(targetKey),
                        careInvitation.status.eq(careInvitationStatus.PENDING)
                )
                .orderBy(careInvitation.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<CareInvitation> findByRequestKeyAndMemberKey(String requestKey, String memberKey) {
        return Optional.ofNullable(
                selectFrom(careInvitation)
                        .where(careInvitation.requestKey.eq(requestKey),
                                careInvitation.targetMemberKey.eq(memberKey))
                        .fetchOne()
        );
    }
}
