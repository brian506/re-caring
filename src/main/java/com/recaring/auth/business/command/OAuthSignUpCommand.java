package com.recaring.auth.business.command;

import com.recaring.auth.vo.NewOauthMember;
import com.recaring.auth.vo.OAuthProvider;
import com.recaring.member.dataaccess.entity.Gender;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.sms.vo.PhoneNumber;

import java.time.LocalDate;

public record OAuthSignUpCommand(
        String providerUserId,
        String smsToken,
        String name,
        LocalDate birth,
        Gender gender,
        MemberRole role
) {
    public NewOauthMember toNewOauthMember(PhoneNumber phone, OAuthProvider provider) {
        return NewOauthMember.builder()
                .phone(phone.value())
                .name(name)
                .birth(birth)
                .gender(gender)
                .role(role)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();
    }
}
