package com.recaring.care.implement;

import com.recaring.care.vo.CareRole;
import com.recaring.care.event.CareInvitationAcceptedEvent;
import com.recaring.care.event.CareInvitationSentEvent;
import com.recaring.care.vo.CareInvitationInfo;
import com.recaring.care.vo.Caregiver;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.care.vo.NewCareInvitation;
import com.recaring.care.vo.Ward;
import com.recaring.member.implement.MemberReader;
import com.recaring.member.vo.MemberInfo;
import com.recaring.sms.vo.PhoneNumber;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;

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
    private final CareInvitationResendLimiter careInvitationResendLimiter;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void sendWardInvitation(String requesterMemberKey, String phoneNumber) {
        MemberInfo ward = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofWardRequest(requesterMemberKey, Ward.of(ward.memberKey(), ward.role()));

        careRelationshipValidator.validateCanAddWard(invitation.requesterMemberKey(), invitation.wardMemberKey());
        validateNoPendingWardRequest(invitation);
        CareInvitationInfo saved = careInvitationWriter.register(invitation);
        eventPublisher.publishEvent(new CareInvitationSentEvent(saved.requestKey(), saved.targetMemberKey(), saved.requesterMemberKey(), saved.targetRole()));
    }

    // 관리자 추가
    @Transactional
    public void sendManagerInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        MemberInfo newManager = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofCaregiverRequest(requesterKey, Caregiver.of(newManager.memberKey(), newManager.role()), wardMemberKey, CareRole.MANAGER);

        careRelationshipValidator.validateCanAddCaregiver(invitation.requesterMemberKey(), invitation.wardMemberKey(), invitation.targetMemberKey());
        validateNoPending(invitation);
        CareInvitationInfo saved = careInvitationWriter.register(invitation);
        eventPublisher.publishEvent(new CareInvitationSentEvent(saved.requestKey(), saved.targetMemberKey(), saved.requesterMemberKey(), saved.targetRole()));
    }

    // 보호자 추가
    @Transactional
    public void sendGuardianInvitation(String requesterKey, String phoneNumber, String wardMemberKey) {
        MemberInfo newGuardian = memberReader.findByPhone(new PhoneNumber(phoneNumber));
        NewCareInvitation invitation = NewCareInvitation.ofCaregiverRequest(requesterKey, Caregiver.of(newGuardian.memberKey(), newGuardian.role()), wardMemberKey, CareRole.GUARDIAN);

        careRelationshipValidator.validateCanAddCaregiver(invitation.requesterMemberKey(), invitation.wardMemberKey(), invitation.targetMemberKey());
        validateNoPending(invitation);
        CareInvitationInfo saved = careInvitationWriter.register(invitation);
        eventPublisher.publishEvent(new CareInvitationSentEvent(saved.requestKey(), saved.targetMemberKey(), saved.requesterMemberKey(), saved.targetRole()));
    }

    // 요청 수락
    @Transactional
    public void accept(String requestKey, String memberKey) {
        CareInvitationInfo invitation = careInvitationReader.findInvitationForRecipient(requestKey, memberKey);
        validatePending(invitation);

        careRelationshipWriter.register(
                new CareRelationshipRegistration(
                        invitation.wardMemberKey(),
                        invitation.caregiverKey(),
                        invitation.careRole()
                )
        );
        careInvitationWriter.accept(invitation.requestKey());
        eventPublisher.publishEvent(new CareInvitationAcceptedEvent(invitation.requestKey(), memberKey, invitation.requesterMemberKey(), invitation.targetRole()));
    }

    @Transactional
    public void reject(String requestKey, String memberKey) {
        CareInvitationInfo request = careInvitationReader.findInvitationForRecipient(requestKey, memberKey);
        validatePending(request);
        careInvitationWriter.reject(request.requestKey());
    }

    @Transactional
    public void resend(String requestKey, String requesterKey) {
        CareInvitationInfo invitation = careInvitationReader.findInvitationForRequester(requestKey, requesterKey);
        validatePending(invitation);
        careInvitationResendLimiter.acquire(invitation.requestKey());
        eventPublisher.publishEvent(new CareInvitationSentEvent(invitation.requestKey(), invitation.targetMemberKey(), invitation.requesterMemberKey(), invitation.targetRole()));
    }

    @Transactional
    public void cancel(String requestKey, String requesterKey) {
        CareInvitationInfo invitation = careInvitationReader.findInvitationForRequester(requestKey, requesterKey);
        validatePending(invitation);
        careInvitationWriter.delete(invitation.requestKey());
    }

    private void validateNoPending(NewCareInvitation invitation) {
        if (careInvitationReader.existsPending(invitation.targetMemberKey(), invitation.wardMemberKey())) {
            throw new AppException(ErrorType.ALREADY_PENDING_CARE_REQUEST);
        }
    }

    // 대상자에게는 여러 보호자가 각자 요청을 보낼 수 있으므로 같은 요청자의 대기 요청만 막는다.
    private void validateNoPendingWardRequest(NewCareInvitation invitation) {
        if (careInvitationReader.existsPendingWardRequest(invitation.requesterMemberKey(), invitation.wardMemberKey())) {
            throw new AppException(ErrorType.ALREADY_PENDING_CARE_REQUEST);
        }
    }

    private void validatePending(CareInvitationInfo invitation) {
        if (!invitation.isPending()) {
            throw new AppException(ErrorType.ALREADY_PROCESSED_CARE_REQUEST);
        }
    }
}
