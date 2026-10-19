package com.recaring.member.controller.response;

import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.entity.SubscriptionType;
import com.recaring.member.vo.MemberInfo;
import com.recaring.member.vo.MemberRole;
import com.recaring.member.vo.TermsAgreementInfo;
import com.recaring.safezone.vo.SafeZoneInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MyInfoResponse(
        String memberKey,
        String name,
        String phone,
        LocalDate birth,
        String gender,
        String profileAvatarCode,
        MemberRole role,
        SubscriptionType subscriptionType,
        SignUpType signUpType,
        LocalDateTime termsServiceAgreedAt,
        LocalDateTime termsPrivacyAgreedAt,
        LocalDateTime termsLocationAgreedAt,
        LocalDateTime joinedAt,
        List<SafeZoneInfo> safeZones
) {
    public static MyInfoResponse of(MemberInfo member, TermsAgreementInfo termsAgreement,
                                    List<SafeZoneInfo> safeZones) {
        return new MyInfoResponse(
                member.memberKey(),
                member.name(),
                member.phone(),
                member.birth(),
                member.gender() != null ? member.gender().getDescription() : null,
                member.profileAvatarCode(),
                member.role(),
                member.subscriptionType(),
                member.signUpType(),
                termsAgreement.termsServiceAgreedAt(),
                termsAgreement.termsPrivacyAgreedAt(),
                termsAgreement.termsLocationAgreedAt(),
                member.joinedAt(),
                safeZones
        );
    }
}
