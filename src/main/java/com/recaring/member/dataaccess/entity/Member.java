package com.recaring.member.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String memberKey;

    @Column(nullable = false, unique = true, length = 11)
    private String phone;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SignUpType signUpType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionType subscriptionType;


    @Builder
    public Member(String phone, String name,
                                LocalDate birth, Gender gender, MemberRole role, SignUpType signUpType) {
        this.memberKey = UUID.randomUUID().toString();
        this.phone = phone;
        this.name = name;
        this.birth = birth;
        this.gender = gender;
        this.role = role;
        this.signUpType = signUpType;
        this.subscriptionType = SubscriptionType.BASIC;
    }

    public void updateProfile(String name, LocalDate birth) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (birth != null) {
            this.birth = birth;
        }
        update();
    }

}
