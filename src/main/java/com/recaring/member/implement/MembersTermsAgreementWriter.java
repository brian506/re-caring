package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.dataaccess.repository.MembersTermsAgreementRepository;
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
