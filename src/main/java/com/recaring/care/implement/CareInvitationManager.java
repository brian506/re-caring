package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareRole;
 import com.recaring.care.event.CareInvitationAcceptedEvent;
import com.recaring.care.event.CareInvitationSentEvent;
import com.recaring.care.vo.Caregiver;
import com.recaring.care.vo.NewCareInvitation;
import com.recaring.care.vo.Ward;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
import com.recaring.sms.vo.PhoneNumber;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void sendWardInvitation(String requesterMemberKey, String phoneNumber) {
        Member ward = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofWardRequest(requesterMemberKey, Ward.of(ward.getMemberKey(), ward.getRole()));

        careRelationshipValidator.validateCanAddWard(invitation.requesterMemberKey(), invitation.wardMemberKey());
        CareInvitation saved = careInvitationWriter.register(invitation);
        eventPublisher.publishEvent(new CareInvitationSentEvent(saved.getRequestKey(), saved.getTargetMemberKey(), saved.getRequesterMemberKey()));
    }

    // 관리자 추가
    @Transactional
    public void sendManagerInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        Member newManager = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofCaregiverRequest(requesterKey, Caregiver.of(newManager.getMemberKey(), newManager.getRole()), wardMemberKey, CareRole.MANAGER);

        careRelationshipValidator.validateCanAddManager(invitation.requesterMemberKey(), invitation.wardMemberKey(), invitation.targetMemberKey());
        CareInvitation saved = careInvitationWriter.register(invitation);
        eventPublisher.publishEvent(new CareInvitationSentEvent(saved.getRequestKey(), saved.getTargetMemberKey(), saved.getRequesterMemberKey()));
    }

    // 보호자 추가
    @Transactional
    public void sendGuardianInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        Member newGuardian = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofCaregiverRequest(requesterKey, Caregiver.of(newGuardian.getMemberKey(), newGuardian.getRole()), wardMemberKey, CareRole.GUARDIAN);

        careRelationshipValidator.validateCanAddGuardian(invitation.requesterMemberKey(), invitation.wardMemberKey(), invitation.targetMemberKey());
        CareInvitation saved = careInvitationWriter.register(invitation);
        eventPublisher.publishEvent(new CareInvitationSentEvent(saved.getRequestKey(), saved.getTargetMemberKey(), saved.getRequesterMemberKey()));
    }

    // 요청 수락
    @Transactional
    public void accept(String requestKey, String memberKey) {
        CareInvitation invitation = careInvitationReader.findByRequestKeyAndMemberKey(requestKey, memberKey);

        careRelationshipWriter.register(invitation, memberKey);
        careInvitationWriter.accept(invitation);
        eventPublisher.publishEvent(new CareInvitationAcceptedEvent(invitation.getRequestKey(), memberKey));
    }

    @Transactional
    public void reject(String requestKey, String memberKey) {
        CareInvitation request = careInvitationReader.findByRequestKeyAndMemberKey(requestKey, memberKey);
        careInvitationWriter.reject(request);
    }
}
