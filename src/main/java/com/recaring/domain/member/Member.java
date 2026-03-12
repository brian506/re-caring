package com.recaring.domain.member;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "members")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false)
    private String memberKey;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 11)
    private String phone;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    public static Member create(String email, String phone, String password, String name, LocalDate birth, Gender gender, MemberRole role) {
        Member member = new Member();
        member.memberKey = UUID.randomUUID().toString();
        member.email = email;
        member.phone = phone;
        member.password = password;
        member.name = name;
        member.birth = birth;
        member.gender = gender;
        member.role = role;
        return member;
    }
}
