package com.recaring.auth.dataaccess.entity;

import com.recaring.auth.vo.OAuthProvider;
import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "oauth",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_member_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id")
    private Long id;

    @Column(nullable = false)
    private String memberKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OAuthProvider provider;

    // 고유 번호
    @Column(name = "provider_member_id", nullable = false)
    private String providerMemberId;

    @Builder
    public OAuth(String memberKey, OAuthProvider provider, String providerMemberId) {
        this.memberKey = memberKey;
        this.provider = provider;
        this.providerMemberId = providerMemberId;
    }

    public static OAuth of(String memberKey, OAuthProvider provider, String providerMemberId) {
        return OAuth.builder()
                .memberKey(memberKey)
                .provider(provider)
                .providerMemberId(providerMemberId)
                .build();
    }
}
