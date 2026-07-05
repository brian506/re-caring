package com.recaring.notification.dataaccess.repository.custom;

import com.recaring.notification.dataaccess.entity.FcmDeviceToken;
import com.recaring.care.dataaccess.entity.CareRole;

import java.util.Collection;
import java.util.List;

public interface FcmDeviceTokenRepositoryCustom {

    void deleteByMemberKey(String memberKey);

    void deleteByTokenIn(Collection<String> tokens);

    List<FcmDeviceToken> findTokensByCareRoles(
            Collection<String> guardianMemberKeys,
            Collection<String> managerMemberKeys
    );
}
