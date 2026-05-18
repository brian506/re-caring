package com.recaring.care.business;

import com.recaring.care.implement.CareInvitationManager;
import com.recaring.care.implement.CareInvitationReader;
import com.recaring.care.vo.ReceivedRequestInfo;
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

    public void sendManagerInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        careInvitationManager.sendManagerInvitation(requesterKey, phoneNumber, wardMemberKey);
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
}
