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

    private final MemberReader memberReader;
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Transactional
    public void registerMember(SignUpCommand command, EncodedPassword encodedPassword, String phone) {
        Member member = memberMapper.toNewMember(command, encodedPassword, phone);
        memberRepository.save(member);
    }

    @Transactional
    public void changePassword(String phone, EncodedPassword encodedPassword) {
        Member member = memberReader.findByPhone(phone);
        member.changePassword(encodedPassword.value());
    }
}
