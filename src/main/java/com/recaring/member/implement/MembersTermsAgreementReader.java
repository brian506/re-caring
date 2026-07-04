package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.dataaccess.repository.MembersTermsAgreementRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembersTermsAgreementReader {

    private final MembersTermsAgreementRepository membersTermsAgreementRepository;

    public MembersTermsAgreement findByMemberKey(String memberKey) {
        return membersTermsAgreementRepository.findByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }
}
