package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CareInvitationReader {

    private final CareInvitationRepository careInvitationRepository;

    public CareInvitation findByRequestKeyAndMemberKey(String requestKey, String memberKey) {
        return careInvitationRepository.findByRequestKeyAndMemberKey(requestKey, memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_REQUEST));
    }

    public List<CareInvitation> findReceivedPendingRequests(String targetKey) {
        return careInvitationRepository.findReceivedPendingRequests(targetKey);
    }
}
