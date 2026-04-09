package com.recaring.member.business;

import com.recaring.member.controller.response.ContactMemberResponse;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberReader memberReader;

    public List<ContactMemberResponse> findByPhoneNumbers(List<String> phoneNumbers) {
        List<Member> members = memberReader.findAllByPhones(phoneNumbers);

        return members.stream()
                .map(ContactMemberResponse::from)
                .toList();
    }
}

