package com.recaring.notification.dataaccess.repository.custom;

public interface FcmDeviceTokenRepositoryCustom {

    void deleteByMemberKey(String memberKey);
}
