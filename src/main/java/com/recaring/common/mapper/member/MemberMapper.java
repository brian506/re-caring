package com.recaring.common.mapper.member;

import com.recaring.auth.vo.NewLocalMember;
import com.recaring.auth.vo.NewOauthMember;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.SignUpType;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public Member toLocalMember(NewLocalMember member) {
        return Member.builder()
                .phone(member.phone().value())
                .name(member.name())
                .birth(member.birth())
                .gender(member.gender())
                .role(member.role())
                .signUpType(SignUpType.LOCAL)
                .build();
    }

    public Member toOAuthMember(NewOauthMember oauthMember) {
        return Member.builder()
                .phone(oauthMember.phone())
                .name(oauthMember.name())
                .birth(oauthMember.birth())
                .gender(oauthMember.gender())
                .role(oauthMember.role())
                .signUpType(SignUpType.OAUTH)
                .build();
    }
}
