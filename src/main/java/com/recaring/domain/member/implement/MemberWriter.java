package com.recaring.domain.member.implement;


import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.domain.member.Member;
import com.recaring.domain.member.infrastructure.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberWriter {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Transactional
    public void registerMember(SignUpCommand command, EncodedPassword encodedPassword) {
        Member member = memberMapper.toNewMember(command, encodedPassword);
        memberRepository.save(member);
    }
}
