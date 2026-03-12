package com.recaring.domain.member.implement;

import com.recaring.domain.member.Member;
import com.recaring.domain.member.infrastructure.repository.MemberRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Member findByMemberKey(String memberKey) {
        return memberRepository.findByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }
}
