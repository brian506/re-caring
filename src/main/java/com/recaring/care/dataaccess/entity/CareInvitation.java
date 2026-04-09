package com.recaring.care.dataaccess.entity;

import com.recaring.care.vo.NewCareInvitation;
import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@Entity
@Table(name = "care_requests")
@SQLDelete(sql = "UPDATE care_requests SET deleted_at = NOW() WHERE care_request_id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_request_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String requestKey;

    @Column(nullable = false)
    private String requesterMemberKey; // 대상자 추가 요청을 보낸 보호자

    @Column(nullable = false)
    private String targetMemberKey; // 요청을 받은 상대 (보호자 or 관리자 or 보호 대상자)

    @Column(nullable = false)
    private String wardMemberKey; // 보호 대상자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareRole careRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareInvitationStatus status;

    @Builder
    public CareInvitation(String requesterMemberKey, String targetMemberKey, String wardMemberKey, String caregiverMemberKey,
                          CareRole careRole) {
        this.requestKey = UUID.randomUUID().toString();
        this.requesterMemberKey = requesterMemberKey;
        this.targetMemberKey = targetMemberKey;
        this.wardMemberKey = wardMemberKey;
        this.careRole = careRole;
        this.status = CareInvitationStatus.PENDING;
    }

    public static CareInvitation from(NewCareInvitation invitation) {
        return CareInvitation.builder()
                .requesterMemberKey(invitation.requesterMemberKey())
                .targetMemberKey(invitation.targetMemberKey())
                .wardMemberKey(invitation.wardMemberKey())
                .careRole(invitation.careRole())
                .build();
    }

    public String getCaregiverKey() {
        if (this.targetMemberKey.equals(this.wardMemberKey)) {
            return this.requesterMemberKey;
        }
        return this.targetMemberKey;
    }

    public void accept() {
        this.status = CareInvitationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = CareInvitationStatus.REJECTED;
    }
}
