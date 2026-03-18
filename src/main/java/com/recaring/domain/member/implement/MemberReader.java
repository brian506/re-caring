package com.recaring.domain.member.implement;

import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.domain.member.dataaccess.repository.MemberRepository;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    public Member findByMemberKey(String memberKey) {
        return memberRepository.findByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Member findByPhone(String phoneNumber) {
        return memberRepository.findByPhone(phoneNumber)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Member findByNameAndBirthAndPhone(String name, LocalDate birth, String phone) {
        return memberRepository.findAccount(name, birth, phone)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }
}
