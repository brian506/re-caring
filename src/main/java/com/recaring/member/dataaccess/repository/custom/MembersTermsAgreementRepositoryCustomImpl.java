package com.recaring.member.dataaccess.repository.custom;

import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.member.dataaccess.entity.QMembersTermsAgreement.membersTermsAgreement;

public class MembersTermsAgreementRepositoryCustomImpl extends QuerydslRepositorySupport
        implements MembersTermsAgreementRepositoryCustom {

    protected MembersTermsAgreementRepositoryCustomImpl() {
        super(MembersTermsAgreement.class);
    }

    @Override
    public void deleteByMemberKey(String memberKey) {
        delete(membersTermsAgreement)
                .where(membersTermsAgreement.memberKey.eq(memberKey))
                .execute();
    }
}
