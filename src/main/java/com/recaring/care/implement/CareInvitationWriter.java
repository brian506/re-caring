package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.care.vo.NewCareInvitation;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CareInvitationWriter {

    private final CareInvitationRepository careInvitationRepository;

    @Transactional
    public CareInvitation register(NewCareInvitation invitation) {
        return careInvitationRepository.save(CareInvitation.from(invitation));
    }

    @Transactional
    public void accept(String requestKey) {
        CareInvitation request = careInvitationRepository.findByRequestKey(requestKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_REQUEST));
        request.accept();
    }

    @Transactional
    public void reject(String requestKey) {
        CareInvitation request = careInvitationRepository.findByRequestKey(requestKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_REQUEST));
        request.reject();
    }

    @Transactional
    public void deleteAllByWardMemberKey(String wardMemberKey) {
        careInvitationRepository.deleteAllByWardMemberKey(wardMemberKey);
    }

    @Transactional
    public void deletePendingSentBy(String wardMemberKey, String requesterMemberKey) {
        careInvitationRepository.deletePendingByWardMemberKeyAndRequesterMemberKey(wardMemberKey, requesterMemberKey);
    }

    @Transactional
    public void deleteAllByMemberKey(String memberKey) {
        careInvitationRepository.deleteAllByMemberKey(memberKey);
    }
}
