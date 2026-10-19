package com.recaring.care.implement;

import com.recaring.care.vo.CareInvitationInfo;
import com.recaring.care.vo.ReceivedRequestInfo;
import com.recaring.care.vo.SentRequestInfo;
import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareInvitationStatus;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.vo.MemberInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class CareInvitationReader {

    private final CareInvitationRepository careInvitationRepository;
    private final MemberReader memberReader;

    public CareInvitationInfo findInvitationForRecipient(String requestKey, String memberKey) {
        return careInvitationRepository.findInvitationForRecipient(requestKey, memberKey)
                .map(CareInvitationInfo::from)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_REQUEST));
    }

    public CareInvitationInfo findInvitationForRequester(String requestKey, String requesterKey) {
        return careInvitationRepository.findInvitationForRequester(requestKey, requesterKey)
                .map(CareInvitationInfo::from)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_REQUEST));
    }

    public boolean existsPending(String targetKey, String wardKey) {
        return careInvitationRepository.existsPending(targetKey, wardKey);
    }

    public boolean existsPendingWardRequest(String requesterKey, String wardKey) {
        return careInvitationRepository.existsPendingWardRequest(requesterKey, wardKey);
    }

    public List<ReceivedRequestInfo> findReceivedRequestInfos(String memberKey) {
        List<CareInvitation> requests = careInvitationRepository.findReceivedPendingRequests(memberKey);

        List<String> keys = requests.stream()
                .flatMap(r -> Stream.of(r.getRequesterMemberKey(), r.getWardMemberKey()))
                .distinct()
                .toList();

        Map<String, MemberInfo> memberMap = memberReader.findAllByMemberKeys(keys);

        return requests.stream()
                .map(r -> ReceivedRequestInfo.of(
                        r,
                        memberMap.get(r.getRequesterMemberKey()),
                        memberMap.get(r.getWardMemberKey())
                ))
                .toList();
    }

    public List<SentRequestInfo> findSentRequestInfos(String requesterKey, CareInvitationStatus status) {
        List<CareInvitation> requests = careInvitationRepository.findSentRequests(requesterKey, status);

        List<String> keys = requests.stream()
                .flatMap(r -> Stream.of(r.getTargetMemberKey(), r.getWardMemberKey()))
                .distinct()
                .toList();

        Map<String, MemberInfo> memberMap = memberReader.findAllByMemberKeys(keys);

        return requests.stream()
                .map(r -> SentRequestInfo.of(
                        r,
                        memberMap.get(r.getTargetMemberKey()),
                        memberMap.get(r.getWardMemberKey())
                ))
                .toList();
    }
}
