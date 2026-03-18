package com.recaring.auth.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalAuth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "local_auth_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String memberKey;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder
    public LocalAuth(String memberKey, String email, String password) {
        this.memberKey = memberKey;
        this.email = email;
        this.password = password;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
