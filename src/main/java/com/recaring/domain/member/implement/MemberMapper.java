package com.recaring.domain.member.implement;

import com.recaring.auth.business.command.SignUpCommand;
import com.recaring.auth.vo.EncodedPassword;
import com.recaring.domain.member.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {
    public Member toNewMember(SignUpCommand command, EncodedPassword encodedPassword) {
        return Member.create(
                command.email().email(),
                encodedPassword.password(),
                command.name(),
                command.birth(),
                command.gender(),
                command.role()
        );
    }
}
