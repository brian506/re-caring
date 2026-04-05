package com.recaring.care.implement;

import com.recaring.care.business.command.AddCaregiverCommand;
import com.recaring.care.business.command.AddWardCommand;
import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.vo.Caregiver;
import com.recaring.care.vo.NewCareInvitation;
import com.recaring.care.vo.Ward;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.implement.MemberReader;
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
    public void sendWardInvitation(AddWardCommand command) {
        Member target = memberReader.findByPhone(command.phoneNumber().value());
        NewCareInvitation invitation = NewCareInvitation.ofWardRequest(command, Ward.from(target));

        careRelationshipValidator.validateCanAdd(invitation.requesterMemberKey(), invitation.wardMemberKey());
        careInvitationWriter.register(invitation);
    }

    @Transactional
    public void sendManagerInvitation(AddCaregiverCommand command) {
        Member target = memberReader.findByPhone(command.phoneNumber().value());
        NewCareInvitation invitation = NewCareInvitation.ofCaregiverRequest(command,Caregiver.from(target));

        careRelationshipValidator.validateCanAdd(invitation.targetMemberKey(), invitation.wardMemberKey());
        careInvitationWriter.register(invitation);
    }

    /**
     * 케어 요청 수락
     */
    @Transactional
    public void accept(String requestKey, String memberKey) {
        CareInvitation invitation = careInvitationReader.findByRequestKeyAndMemberKey(requestKey, memberKey);

        careRelationshipWriter.register(invitation, memberKey);
        careInvitationWriter.accept(invitation);
    }

    /**
     * 케어 요청 거절
     */
    @Transactional
    public void reject(String requestKey, String memberKey) {
        CareInvitation request = careInvitationReader.findByRequestKeyAndMemberKey(requestKey, memberKey);
        careInvitationWriter.reject(request);
    }


}
