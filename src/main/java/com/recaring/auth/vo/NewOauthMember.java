package com.recaring.auth.vo;

import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.MemberRole;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record NewOauthMember(
        String phone,
        String name,
        LocalDate birth,
        Gender gender,
        MemberRole role,
        OAuthProvider provider,
        String providerMemberId
) {
}
