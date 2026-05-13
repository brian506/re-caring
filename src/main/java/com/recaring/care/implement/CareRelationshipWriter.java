package com.recaring.care.implement;

import com.recaring.care.dataaccess.entity.CareRelationship;
import com.recaring.care.dataaccess.repository.CareRelationshipRepository;
import com.recaring.care.vo.CareRelationshipRegistration;
import com.recaring.member.dataaccess.entity.Member;
import com.recaring.member.dataaccess.entity.MemberRole;
import com.recaring.member.implement.MemberReader;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CareRelationshipWriter {

    private final CareRelationshipRepository careRelationshipRepository;
    private final MemberReader memberReader;
    private final CareRelationshipValidator relationshipValidator;

    @Transactional
    public void register(CareRelationshipRegistration registration, String memberKey) {
        Member member = memberReader.findMemberByLock(memberKey);
        if (member.getRole() == MemberRole.GUARDIAN) {
            relationshipValidator.validateCanAddWard(memberKey, registration.wardMemberKey());
        }
        //todo 보호 대상자의 보호자 수 제한?
        careRelationshipRepository.save(
                CareRelationship.of(registration.wardMemberKey(), registration.caregiverKey(), registration.careRole())
        );
    }

    @Transactional
    public void delete(String wardKey, String caregiverKey) {
        CareRelationship relationship = careRelationshipRepository
                .findByWardKeyAndCaregiverKey(wardKey, caregiverKey)
                .orElseThrow(() -> new AppException(ErrorType.NOT_FOUND_CARE_RELATIONSHIP));
        relationship.delete();
    }
}
