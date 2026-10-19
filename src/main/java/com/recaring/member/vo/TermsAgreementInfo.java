package com.recaring.member.vo;

import com.recaring.member.dataaccess.entity.MembersTermsAgreement;

import java.time.LocalDateTime;

public record TermsAgreementInfo(
        LocalDateTime termsServiceAgreedAt,
        LocalDateTime termsPrivacyAgreedAt,
        LocalDateTime termsLocationAgreedAt
) {
    public static TermsAgreementInfo from(MembersTermsAgreement entity) {
        return new TermsAgreementInfo(
                entity.getTermsServiceAgreedAt(),
                entity.getTermsPrivacyAgreedAt(),
                entity.getTermsLocationAgreedAt()
        );
    }
}
