package com.recaring.member.vo;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.entity.SubscriptionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberInfo(
        String memberKey,
        String name,
        String phone,
        LocalDate birth,
        Gender gender,
        MemberRole role,
        SignUpType signUpType,
        SubscriptionType subscriptionType,
        String profileAvatarCode,
        LocalDateTime joinedAt
) {
    public static MemberInfo from(Member entity) {
        return new MemberInfo(
                entity.getMemberKey(),
                entity.getName(),
                entity.getPhone(),
                entity.getBirth(),
                entity.getGender(),
                entity.getRole(),
                entity.getSignUpType(),
                entity.getSubscriptionType(),
                entity.getProfileAvatarCode(),
                entity.getCreatedAt()
        );
    }
}
