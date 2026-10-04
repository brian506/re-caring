package com.recaring.care.dataaccess.repository.custom;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;

import java.util.List;
import java.util.Optional;

public interface CareInvitationRepositoryCustom {

    List<CareInvitation> findReceivedPendingRequests(String targetKey);

    Optional<CareInvitation> findInvitationForRecipient(String requestKey, String memberKey);

    List<CareInvitation> findSentRequests(String requesterKey, CareInvitationStatus status);

    Optional<CareInvitation> findInvitationForRequester(String requestKey, String requesterKey);

    boolean existsPending(String targetKey, String wardKey);

    void deleteAllByMemberKey(String memberKey);

    void deleteAllByWardMemberKey(String wardMemberKey);

    void deletePendingByWardMemberKeyAndRequesterMemberKey(String wardMemberKey, String requesterMemberKey);
}
