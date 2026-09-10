package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.DesignatedAvatar;
import com.recaring.support.repository.QuerydslRepositorySupport;

import static com.recaring.care.dataaccess.entity.QDesignatedAvatar.designatedAvatar;

public class DesignatedAvatarRepositoryCustomImpl extends QuerydslRepositorySupport
        implements DesignatedAvatarRepositoryCustom {

    protected DesignatedAvatarRepositoryCustomImpl() {
        super(DesignatedAvatar.class);
    }

    @Override
    public void deleteAllByRelationship(String wardMemberKey, String caregiverMemberKey) {
        delete(designatedAvatar)
                .where(
                        designatedAvatar.wardMemberKey.eq(wardMemberKey),
                        designatedAvatar.ownerMemberKey.eq(caregiverMemberKey)
                                .or(designatedAvatar.targetMemberKey.eq(caregiverMemberKey))
                )
                .execute();
    }

    @Override
    public void deleteAllByMemberKey(String memberKey) {
        delete(designatedAvatar)
                .where(
                        designatedAvatar.ownerMemberKey.eq(memberKey)
                                .or(designatedAvatar.wardMemberKey.eq(memberKey))
                                .or(designatedAvatar.targetMemberKey.eq(memberKey))
                )
                .execute();
    }
}
