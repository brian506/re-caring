package com.recaring.care.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 케어 관계 안에서 한 회원이 다른 회원에게 붙인 프로필 아바타. 붙인 사람에게만 보인다.
 * 회원 전역 필드(Member.profileAvatarCode)에 쓰면 본인이 고른 얼굴을 덮어쓰고 다른 보호자에게도 보이므로 따로 둔다.
 * 대상자 얼굴은 target = ward, 보호자·관계자 얼굴은 target = 그 사람이며 둘 다 ward 범위 안에서만 유효하다.
 */
@Getter
@Entity
@Table(
        name = "designated_avatars",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_member_key", "ward_member_key", "target_member_key"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DesignatedAvatar extends BaseEntity {

    // TODO: CREATE INDEX idx_designated_avatars_ward ON designated_avatars(ward_member_key);
    // TODO: CREATE INDEX idx_designated_avatars_target ON designated_avatars(target_member_key);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "designated_avatar_id")
    private Long id;

    @Column(nullable = false)
    private String ownerMemberKey; // 얼굴을 지정한 사람

    @Column(nullable = false)
    private String wardMemberKey; // 케어 범위

    @Column(nullable = false)
    private String targetMemberKey; // 얼굴이 붙는 사람

    @Column(nullable = false, length = 30)
    private String profileAvatarCode;

    @Builder
    public DesignatedAvatar(String ownerMemberKey, String wardMemberKey, String targetMemberKey, String profileAvatarCode) {
        this.ownerMemberKey = ownerMemberKey;
        this.wardMemberKey = wardMemberKey;
        this.targetMemberKey = targetMemberKey;
        this.profileAvatarCode = profileAvatarCode;
    }

    public void changeProfileAvatarCode(String profileAvatarCode) {
        this.profileAvatarCode = profileAvatarCode;
        update();
    }
}
