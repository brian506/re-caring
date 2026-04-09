package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.vo.Caregiver;
import com.recaring.care.vo.NewCareInvitation;
import com.recaring.care.vo.Ward;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.sms.vo.PhoneNumber;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CareInvitationManager {

    private final MemberReader memberReader;
    private final CareInvitationReader careInvitationReader;
    private final CareInvitationWriter careInvitationWriter;
    private final CareRelationshipWriter careRelationshipWriter;
    private final CareRelationshipValidator careRelationshipValidator;

    @Transactional
    public void sendWardInvitation(String requesterMemberKey, String phoneNumber) {
        Member ward = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofWardRequest(requesterMemberKey, Ward.from(ward));

        careRelationshipValidator.validateCanAddWard(invitation.requesterMemberKey(), invitation.wardMemberKey());
        careInvitationWriter.register(invitation);
    }

    // 관리자 추가
    @Transactional
    public void sendManagerInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        Member newManager = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofCaregiverRequest(requesterKey, Caregiver.from(newManager), wardMemberKey, CareRole.MANAGER);

        careRelationshipValidator.validateCanAddManager(invitation.requesterMemberKey(), invitation.wardMemberKey());
        careInvitationWriter.register(invitation);
    }

    // 보호자 추가
    @Transactional
    public void sendGuardianInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        Member newGuardian = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofCaregiverRequest(requesterKey, Caregiver.from(newGuardian), wardMemberKey, CareRole.GUARDIAN);

        careRelationshipValidator.validateCanAddGuardian(invitation.requesterMemberKey(), invitation.wardMemberKey());
        careInvitationWriter.register(invitation);
    }

    // 요청 수락
    @Transactional
    public void accept(String requestKey, String memberKey) {
        CareInvitation invitation = careInvitationReader.findByRequestKeyAndMemberKey(requestKey, memberKey);

        careRelationshipWriter.register(invitation, memberKey);
        careInvitationWriter.accept(invitation);
    }

    @Transactional
    public void reject(String requestKey, String memberKey) {
        CareInvitation request = careInvitationReader.findByRequestKeyAndMemberKey(requestKey, memberKey);
        careInvitationWriter.reject(request);
    }


}
