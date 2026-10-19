package com.recaring.member.implement;

import com.recaring.member.dataaccess.repository.MemberRepository;
import com.recaring.member.vo.MemberInfo;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    public MemberInfo findByMemberKey(String memberKey) {
        return memberRepository.findByMemberKey(memberKey)
                .map(MemberInfo::from)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public String findNameByMemberKey(String memberKey) {
        return findByMemberKey(memberKey).name();
    }

    public MemberInfo findByPhone(PhoneNumber phoneNumber) {
        return findOptionalByPhone(phoneNumber)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }

    public Optional<MemberInfo> findOptionalByPhone(PhoneNumber phoneNumber) {
        return memberRepository.findByPhone(phoneNumber.value()).map(MemberInfo::from);
    }

    public boolean existsByPhone(PhoneNumber phoneNumber) {
        return memberRepository.existsByPhone(phoneNumber.value());
    }

    public List<MemberInfo> findByPhones(List<String> phones) {
        return memberRepository.findByPhones(phones)
                .stream()
                .map(MemberInfo::from)
                .toList();
    }

    public Map<String, MemberInfo> findAllByMemberKeys(List<String> memberKeys) {
        return memberRepository.findAllByMemberKeyIn(memberKeys)
                .stream()
                .map(MemberInfo::from)
                .collect(Collectors.toMap(MemberInfo::memberKey, m -> m));
    }

    public void lockForUpdate(String memberKey) {
        memberRepository.findForUpdate(memberKey).orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_ACCOUNT));
    }
}
