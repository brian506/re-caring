package com.recaring.member.implement;

import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    public Member findByMemberKey(String memberKey) {
        return memberRepository.findByMemberKey(memberKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Member findByPhone(PhoneNumber phoneNumber) {
        return memberRepository.findByPhone(phoneNumber.value())
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Member findEmail(String name, LocalDate birth, String phone) {
        return memberRepository.findAccount(name, birth, phone)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public List<Member> findAllByPhones(List<String> phones) {
        return memberRepository.findAllByPhoneIn(phones);
    }

    public Map<String, Member> findAllByMemberKeys(List<String> memberKeys) {
        return memberRepository.findAllByMemberKeyIn(memberKeys)
                .stream()
                .collect(Collectors.toMap(Member::getMemberKey, m -> m));
    }

    public Member findMemberByLock(String memberKey) {
        return memberRepository.findMemberByPessimistic(memberKey).orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }
}
