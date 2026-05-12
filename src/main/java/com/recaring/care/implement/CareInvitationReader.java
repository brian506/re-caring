package com.recaring.care.implement;

import com.recaring.care.vo.ReceivedRequestInfo;
import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
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

    public CareInvitation findByRequestKeyAndMemberKey(String requestKey, String memberKey) {
        return careInvitationRepository.findByRequestKeyAndMemberKey(requestKey, memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_REQUEST));
    }

    public List<ReceivedRequestInfo> findReceivedRequestInfos(String memberKey) {
        List<CareInvitation> requests = careInvitationRepository.findReceivedPendingRequests(memberKey);

        List<String> keys = requests.stream()
                .flatMap(r -> Stream.of(r.getRequesterMemberKey(), r.getWardMemberKey()))
                .distinct()
                .toList();

        Map<String, Member> memberMap = memberReader.findAllByMemberKeys(keys);

        return requests.stream()
                .map(r -> ReceivedRequestInfo.of(
                        r,
                        memberMap.get(r.getRequesterMemberKey()),
                        memberMap.get(r.getWardMemberKey())
                ))
                .toList();
    }
}
