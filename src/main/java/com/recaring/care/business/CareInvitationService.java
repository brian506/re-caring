package com.recaring.care.business;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.implement.CareInvitationManager;
import com.recaring.care.implement.CareInvitationReader;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.sms.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CareInvitationService {

    private final CareInvitationManager careInvitationManager;
    private final CareInvitationReader careInvitationReader;
    private final MemberReader memberReader;

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
        List<CareInvitation> requests = careInvitationReader.findReceivedPendingRequests(memberKey);

        List<String> memberKeys = requests.stream()
                .flatMap(r -> Stream.of(r.getRequesterMemberKey(), r.getWardMemberKey()))
                .distinct()
                .toList();

        Map<String, Member> memberMap = memberReader.findAllByMemberKeys(memberKeys);

        return requests.stream()
                .map(r -> {
                    Member requester = memberMap.get(r.getRequesterMemberKey());
                    Member ward = memberMap.get(r.getWardMemberKey());
                    return new ReceivedRequestInfo(
                            r.getRequestKey(),
                            requester.getMemberKey(),
                            requester.getName(),
                            requester.getPhone(),
                            ward.getMemberKey(),
                            ward.getName(),
                            r.getCareRole(),
                            r.getStatus(),
                            r.getExpiredAt(),
                            r.getCreatedAt()
                    );
                })
                .toList();
    }

    public void accept(String requestKey, String memberKey) {
        careInvitationManager.accept(requestKey, memberKey);
    }

    public void reject(String requestKey, String memberKey) {
        careInvitationManager.reject(requestKey, memberKey);
    }
}
