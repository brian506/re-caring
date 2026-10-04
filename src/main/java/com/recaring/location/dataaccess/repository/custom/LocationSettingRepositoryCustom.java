package com.recaring.location.dataaccess.repository.custom;

public interface LocationSettingRepositoryCustom {

    void deleteByMemberKey(String memberKey);

    void deleteByRelationship(String wardMemberKey, String caregiverMemberKey);
}
