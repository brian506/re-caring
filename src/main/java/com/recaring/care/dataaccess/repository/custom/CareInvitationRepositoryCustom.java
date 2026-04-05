package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareInvitation;

import java.util.List;
import java.util.Optional;

public interface CareInvitationRepositoryCustom {

    List<CareInvitation> findReceivedPendingRequests(String targetKey);

    Optional<CareInvitation> findByRequestKeyAndMemberKey(String requestKey, String memberKey);
}
