package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.util.List;
import java.util.Optional;

import static com.recaring.care.dataaccess.entity.QCareInvitation.careInvitation;

public class CareInvitationRepositoryCustomImpl extends QuerydslRepositorySupport
        implements CareInvitationRepositoryCustom {

    protected CareInvitationRepositoryCustomImpl() {
        super(CareInvitation.class);
    }

    @Override
    public List<CareInvitation> findReceivedPendingRequests(String targetKey) {
        return selectFrom(careInvitation)
                .where(
                        careInvitation.targetMemberKey.eq(targetKey),
                        careInvitation.status.eq(CareInvitationStatus.PENDING)
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
