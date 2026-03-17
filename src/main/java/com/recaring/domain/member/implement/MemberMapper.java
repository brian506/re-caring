package com.recaring.domain.member.implement;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.domain.member.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public Member toNewMember(SignUpCommand command, EncodedPassword encodedPassword, String phone) {
        return Member.create(
                command.email().email(),
                phone,
                encodedPassword.value(),
                command.name(),
                command.birth(),
                command.gender(),
                command.role()
        );
    }
}
