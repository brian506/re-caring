package com.recaring.member.implement;

import com.recaring.auth.vo.NewLocalMember;
import com.recaring.auth.vo.NewOauthMember;
import com.recaring.common.mapper.member.MemberMapper;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberWriter {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Transactional
    public String registerLocalMember(NewLocalMember newLocalMember) {
        Member member = memberMapper.toLocalMember(newLocalMember);
        return memberRepository.save(member).getMemberKey();
    }

    @Transactional
    public String registerOAuthMember(NewOauthMember newOauthMember) {
        Member member = memberMapper.toOAuthMember(newOauthMember);
        return memberRepository.save(member).getMemberKey();
    }
}
