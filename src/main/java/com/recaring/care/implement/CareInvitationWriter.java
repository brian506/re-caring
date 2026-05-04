package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.care.vo.NewCareInvitation;
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
    public void accept(CareInvitation request) {
        request.accept();
    }

    @Transactional
    public void reject(CareInvitation request) {
        request.reject();
    }
}
