package com.recaring.care.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "care_relationships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ward_member_key", "caregiver_member_key"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareRelationship extends BaseEntity {

    public static final int MAX_WARD_NICKNAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_relationship_id")
    private Long id;

    @Column(nullable = false)
    private String wardMemberKey; // 보호 대상자

    @Column(nullable = false)
    private String caregiverMemberKey; // 보호자 or 관계자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareRole careRole;

    @Column(length = MAX_WARD_NICKNAME_LENGTH)
    private String wardNickname; // null이면 대상자 실명을 그대로 쓴다

    @Builder
    public CareRelationship(String wardMemberKey, String caregiverMemberKey, CareRole careRole) {
        this.wardMemberKey = wardMemberKey;
        this.caregiverMemberKey = caregiverMemberKey;
        this.careRole = careRole;
    }

    public static CareRelationship of(String wardMemberKey, String caregiverMemberKey, CareRole careRole) {
        return CareRelationship.builder()
                .wardMemberKey(wardMemberKey)
                .caregiverMemberKey(caregiverMemberKey)
                .careRole(careRole)
                .build();
    }

    public void changeWardNickname(String wardNickname) {
        this.wardNickname = wardNickname;
    }

    public void changeCareRole(CareRole careRole) {
        this.careRole = careRole;
    }
}
