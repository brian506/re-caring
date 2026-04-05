package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.repository.CareInvitationRepository;
import com.recaring.care.vo.NewCareInvitation;
import com.recaring.common.mapper.care.CareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CareInvitationWriter {

    private final CareInvitationRepository careInvitationRepository;
    private final CareMapper careMapper;

    @Transactional
    public void register(NewCareInvitation invitation) {
        careInvitationRepository.save(careMapper.toCareInvitation(invitation));
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
