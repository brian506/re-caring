package com.recaring.domain.member.dataaccess.repository.custom;

import com.recaring.domain.member.dataaccess.entity.Member;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.time.LocalDate;
import java.util.Optional;

import static com.recaring.domain.member.dataaccess.entity.QMember.member;

public class MemberRepositoryCustomImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {

    protected MemberRepositoryCustomImpl() {
        super(Member.class);
    }

    @Override
    public Optional<Member> findAccount(String name, LocalDate birth, String phone) {
        return Optional.ofNullable(
                selectFrom(member)
                .where(
                        member.name.eq(name),
                        member.birth.eq(birth),
                        member.phone.eq(phone)
                ).fetchOne()
        );
    }

}
