package com.recaring.domain.member.infrastructure.repository.custom;

import com.recaring.domain.member.Member;
import com.recaring.support.repository.QuerydslRepositorySupport;

import java.time.LocalDate;
import java.util.Optional;

import static com.recaring.domain.member.QMember.member;

public class MemberRepositoryCustomImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {

    protected MemberRepositoryCustomImpl(Class<?> entityClass) {
        super(entityClass);
    }


    @Override
    public Optional<Member> findByNameAndBirthAndPhone(String name, LocalDate birth, String phone) {
        return selectFrom(member)
                .where(
                        member.name.eq(name),
                        member.birth.eq(birth),
                        member.phone.eq(phone)
                )
                .fetchOptional();
    }
}
