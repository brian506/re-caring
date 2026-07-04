package com.recaring.member.implement;

import com.recaring.auth.vo.NewLocalMember;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.SignUpType;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
    public void updateProfile(String memberKey, String name, LocalDate birth) {
        Member member = memberRepository.findByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
        member.updateProfile(name, birth);
    }

    @Transactional
    public void deleteByMemberKey(String memberKey) {
        memberRepository.deleteByMemberKey(memberKey);
    }
}
