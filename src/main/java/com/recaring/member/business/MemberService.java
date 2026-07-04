package com.recaring.member.business;

import com.recaring.auth.implement.local.LocalAuthReader;
import com.recaring.auth.vo.Password;
import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.member.controller.response.MyInfoResponse;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MembersTermsAgreement;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.implement.MembersTermsAgreementReader;
import com.recaring.member.implement.MemberWithdrawalManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberReader memberReader;
    private final LocalAuthReader localAuthReader;
    private final MembersTermsAgreementReader membersTermsAgreementReader;
    private final MemberWithdrawalManager memberWithdrawalManager;

    public List<ContactMemberResponse> findByPhoneNumbers(List<String> phoneNumbers) {
        List<Member> members = memberReader.findAllByPhones(phoneNumbers);

        return members.stream()
                .map(ContactMemberResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(String memberKey) {
        Member member = memberReader.findByMemberKey(memberKey);
        String email = localAuthReader.findEmailByMemberKey(memberKey);
        MembersTermsAgreement termsAgreement = membersTermsAgreementReader.findByMemberKey(memberKey);
        return MyInfoResponse.of(member, email, termsAgreement);
    }

    public void withdraw(String memberKey, Password password) {
        memberWithdrawalManager.withdraw(memberKey, password);
    }
}
