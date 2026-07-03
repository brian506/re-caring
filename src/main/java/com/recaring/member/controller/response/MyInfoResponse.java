package com.recaring.member.controller.response;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.entity.SubscriptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyInfoResponse(
        String memberKey,
        String name,
        String phone,
        LocalDate birth,
        Gender gender,
        MemberRole role,
        SubscriptionType subscriptionType,
        SignUpType signUpType,
        String email,
        LocalDateTime termsServiceAgreedAt,
        LocalDateTime termsPrivacyAgreedAt,
        LocalDateTime termsLocationAgreedAt
) {
    public static MyInfoResponse of(Member member, String email, MembersTermsAgreement termsAgreement) {
        return new MyInfoResponse(
                member.getMemberKey(),
                member.getName(),
                member.getPhone(),
                member.getBirth(),
                member.getGender(),
                member.getRole(),
                member.getSubscriptionType(),
                member.getSignUpType(),
                email,
                termsAgreement.getTermsServiceAgreedAt(),
                termsAgreement.getTermsPrivacyAgreedAt(),
                termsAgreement.getTermsLocationAgreedAt()
        );
    }
}
