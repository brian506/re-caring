package com.recaring.domain.member.implement;

import com.recaring.domain.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.domain.member.dataaccess.repository.MembersTermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MembersTermsAgreementWriter {

    private final MembersTermsAgreementRepository membersTermsAgreementRepository;

    @Transactional
    public void register(String memberKey) {
        membersTermsAgreementRepository.save(
                MembersTermsAgreement.builder()
                        .memberKey(memberKey)
                        .build()
        );
    }
}
