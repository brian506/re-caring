package com.recaring.member.dataaccess.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "members_terms_agreements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembersTermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terms_agreement_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String memberKey;

    @Column(nullable = false)
    private LocalDateTime termsServiceAgreedAt;

    @Column(nullable = false)
    private LocalDateTime termsPrivacyAgreedAt;

    @Column(nullable = false)
    private LocalDateTime termsLocationAgreedAt;

    @Builder
    public MembersTermsAgreement(String memberKey) {
        this.memberKey = memberKey;
        this.termsServiceAgreedAt = LocalDateTime.now();
        this.termsPrivacyAgreedAt = LocalDateTime.now();
        this.termsLocationAgreedAt = LocalDateTime.now();
    }
}
