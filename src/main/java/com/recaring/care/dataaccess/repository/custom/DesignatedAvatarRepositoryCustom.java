package com.recaring.care.dataaccess.repository.custom;

public interface DesignatedAvatarRepositoryCustom {

    void deleteAllByRelationship(String wardMemberKey, String caregiverMemberKey);

    void deleteAllByMemberKey(String memberKey);
}
