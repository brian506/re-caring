package com.recaring.domain.member.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private LocalDateTime termsServiceAgreedAt;

    @Column(nullable = false)
    private LocalDateTime termsPrivacyAgreedAt;

    @Column(nullable = false)
    private LocalDateTime termsLocationAgreedAt;

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
        this.termsServiceAgreedAt = LocalDateTime.now();
        this.termsPrivacyAgreedAt = LocalDateTime.now();
        this.termsLocationAgreedAt = LocalDateTime.now();
    }

}
