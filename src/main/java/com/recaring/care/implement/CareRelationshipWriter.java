package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareInvitation;
import com.recaring.care.dataaccess.entity.CareRole;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.common.mapper.care.CareMapper;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.implement.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CareRelationshipWriter {

    private final CareRelationshipRepository careRelationshipRepository;
    private final CareMapper careMapper;
    private final MemberReader memberReader;
    private final CareRelationshipValidator relationshipValidator;

    @Transactional
    public void register(CareInvitation invitation, String memberKey) {
        Member member =  memberReader.findMemberByLock(memberKey);
        if(member.getRole() == MemberRole.GUARDIAN) {
            relationshipValidator.validateCanAdd(memberKey,invitation.getWardMemberKey());
        }
        //todo 보호 대상자의 보호자 수 제한?
        careRelationshipRepository.save(careMapper.toCareRelationship(invitation.getWardMemberKey(),invitation.getCaregiverKey(), invitation.getCareRole()));
    }
}
