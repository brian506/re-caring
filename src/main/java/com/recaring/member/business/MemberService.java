package com.recaring.member.business;

import com.recaring.auth.vo.Password;
import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MemberWithdrawalManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberReader memberReader;
    private final MemberWithdrawalManager memberWithdrawalManager;

    public List<ContactMemberResponse> findByPhoneNumbers(List<String> phoneNumbers) {
        List<Member> members = memberReader.findAllByPhones(phoneNumbers);

        return members.stream()
                .map(ContactMemberResponse::from)
                .toList();
    }

    public void withdraw(String memberKey, Password password) {
        memberWithdrawalManager.withdraw(memberKey, password);
    }
}

