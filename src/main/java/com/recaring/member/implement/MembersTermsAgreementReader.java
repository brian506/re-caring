package com.recaring.member.implement;

import com.recaring.member.dataaccess.repository.MembersTermsAgreementRepository;
import com.recaring.member.vo.TermsAgreementInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembersTermsAgreementReader {

    private final MembersTermsAgreementRepository membersTermsAgreementRepository;

    public TermsAgreementInfo findByMemberKey(String memberKey) {
        return membersTermsAgreementRepository.findByMemberKey(memberKey)
                .map(TermsAgreementInfo::from)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }
}
