package com.recaring.care.dataaccess.entity;

import com.recaring.care.vo.NewCareInvitation;
import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "care_invitation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_invitation_id")
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

    /**
     * 초대 대상(target)이 어떤 역할로 요청받았는지 반환한다.
     * target == ward 이면 보호 대상자, 그 외에는 careRole(보호자/관계자)이다.
     */
    public CarePartyRole targetRole() {
        if (this.targetMemberKey.equals(this.wardMemberKey)) {
            return CarePartyRole.WARD;
        }
        return this.careRole == CareRole.MANAGER ? CarePartyRole.MANAGER : CarePartyRole.GUARDIAN;
    }

    public void accept() {
        this.status = CareInvitationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = CareInvitationStatus.REJECTED;
    }
}
