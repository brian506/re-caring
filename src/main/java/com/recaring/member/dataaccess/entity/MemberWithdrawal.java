package com.recaring.member.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// TODO: CREATE INDEX idx_member_withdrawal_member_key ON member_withdrawal(member_key);
@Getter
@Entity
@Table(name = "member_withdrawal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_withdrawal_id")
    private Long id;

    @Column(name = "member_key", nullable = false)
    private String memberKey;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    private MemberWithdrawal(String memberKey, String email, MemberRole role) {
        this.memberKey = memberKey;
        this.email = email;
        this.role = role;
        this.withdrawnAt = LocalDateTime.now();
    }

    public static MemberWithdrawal of(String memberKey, String email, MemberRole role) {
        return new MemberWithdrawal(memberKey, email, role);
    }
}
