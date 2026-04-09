package com.recaring.member.implement;

import com.recaring.auth.vo.NewLocalMember;
import com.recaring.auth.vo.NewOauthMember;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberWriter {

    private final MemberRepository memberRepository;

    @Transactional
    public String registerLocalMember(NewLocalMember newLocalMember) {
        Member member = Member.builder()
                .phone(newLocalMember.phone().value())
                .name(newLocalMember.name())
                .birth(newLocalMember.birth())
                .gender(newLocalMember.gender())
                .role(newLocalMember.role())
                .signUpType(SignUpType.LOCAL)
                .build();
        return memberRepository.save(member).getMemberKey();
    }

    @Transactional
    public String registerOAuthMember(NewOauthMember newOauthMember) {
        Member member = Member.builder()
                .phone(newOauthMember.phone())
                .name(newOauthMember.name())
                .birth(newOauthMember.birth())
                .gender(newOauthMember.gender())
                .role(newOauthMember.role())
                .signUpType(SignUpType.OAUTH)
                .build();
        return memberRepository.save(member).getMemberKey();
    }
}
