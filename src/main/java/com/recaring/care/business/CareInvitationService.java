package com.recaring.care.business;

import com.recaring.care.implement.CareInvitationManager;
import com.recaring.care.implement.CareInvitationReader;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.vo.ReceivedRequestInfo;
import com.recaring.care.vo.SentRequestInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareInvitationService {

    private final CareInvitationManager careInvitationManager;
    private final CareInvitationReader careInvitationReader;

    public void sendWardInvitation(String requesterKey, String phoneNumber) {
        careInvitationManager.sendWardInvitation(requesterKey, phoneNumber);
    }

    public void sendGuardianInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        careInvitationManager.sendGuardianInvitation(requesterKey, phoneNumber, wardMemberKey);
    }

    public List<ReceivedRequestInfo> getReceivedRequests(String memberKey) {
        return careInvitationReader.findReceivedRequestInfos(memberKey);
    }

    public void accept(String requestKey, String memberKey) {
        careInvitationManager.accept(requestKey, memberKey);
    }

    public void reject(String requestKey, String memberKey) {
        careInvitationManager.reject(requestKey, memberKey);
    }

    public List<SentRequestInfo> getSentRequests(String requesterKey, CareInvitationStatus status) {
        return careInvitationReader.findSentRequestInfos(requesterKey, status);
    }

    public void resend(String requestKey, String requesterKey) {
        careInvitationManager.resend(requestKey, requesterKey);
    }

    public void cancel(String requestKey, String requesterKey) {
        careInvitationManager.cancel(requestKey, requesterKey);
    }
}
